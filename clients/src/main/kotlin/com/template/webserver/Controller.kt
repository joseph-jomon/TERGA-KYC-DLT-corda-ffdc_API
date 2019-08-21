package com.template.webserver


import com.template.flows.KycIssueFlow
import com.template.flows.KycUpdateFlow
import com.template.states.KycState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/template/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy
    /**
     * Returns the node's name.
     */


    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GetMapping(value = [ "kyc-in-vault" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getkyc() : ResponseEntity<List<StateAndRef<KycState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<KycState>().states)
    }

    /**
     * Initiates a flow to agree a Kyc between two parties.
     *
     * Once the flow finishes it will have written the KYC to ledger. Both the owner and the bank will be able to
     * see it when calling /spring/api/kyc on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */

    @PostMapping(value = [ "create-kyc" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createKyc(request: HttpServletRequest): ResponseEntity<String> {
        val kycname= request.getParameter("kname")
        val kycaddress = request.getParameter("kaddress")
        val kycdob = request.getParameter("kdob")
        val  kycemail = request.getParameter("kemail")
        val  partyName = request.getParameter("partyName")
        if(partyName == null){
            return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        }

        val partyX500Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(::KycIssueFlow, kycname,kycaddress,kycdob, kycemail, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
    //*****************************************************************************************************************************************************************************

    @PostMapping(value = [ "update-kyc" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun updateKyc(request: HttpServletRequest): ResponseEntity<String> {
        val kycname= request.getParameter("kname")
        val kycaddress = request.getParameter("kaddress")
        val kycdob = request.getParameter("kdob")
        val  kycemail = request.getParameter("kemail")
        val  partyName = request.getParameter("partyName")
        val  kycidastring = request.getParameter("KycId")
        if(partyName == null){
            return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        }

        val partyX500Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")

        return try {
            var signedTx = proxy.startTrackedFlow(::KycUpdateFlow, kycname,kycaddress,kycdob, kycemail, otherParty, UniqueIdentifier.fromString(kycidastring)).returnValue.getOrThrow()
            logger.info("signedTx ========= "+signedTx);
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
            //ResponseEntity.status(HttpStatus.CREATED).body("Transaction id committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    //*****************************************************************************************************************************************************************************

    /**
     * Displays all Kyc states that only this node has been involved in.
     */
    @GetMapping(value = [ "my-kycs" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getMykycs(): ResponseEntity<List<StateAndRef<KycState>>>  {
        val mykyc = proxy.vaultQueryBy<KycState>().states.filter { it.state.data.owner.equals(proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(mykyc)
    }
    //*******************************************************************************************************************************************************************************************
    @GetMapping(value = [ "myallkyc"], produces = [ APPLICATION_JSON_VALUE ])
    fun getMyallkycs(): ResponseEntity<Vault.Page<KycState>>? {
        val linearStateCriteria = QueryCriteria.LinearStateQueryCriteria( status = Vault.StateStatus.ALL)
        val vaultCriteria = VaultQueryCriteria(status = Vault.StateStatus.ALL)
        val results = proxy.vaultQueryBy<KycState>(linearStateCriteria and vaultCriteria)
        return ResponseEntity.ok(results)  }

}