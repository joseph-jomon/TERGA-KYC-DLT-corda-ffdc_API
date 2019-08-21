package com.template.flows



import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KycContract
import com.template.states.KycState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class KycUpdateFlow(val kname: String = "not_shared",val kaddress: String = "not_shared",val kdob: String = "not_shared",val kemail: String = "not_shared", val otherParty: Party,val KycId : UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():SignedTransaction {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We create the transaction components.
        val outputState = KycState(kname,kaddress,kdob,kemail, ourIdentity, otherParty,KycId)


            val command = Command(KycContract.Commands.UpdateKyc(), listOf(ourIdentity.owningKey, otherParty.owningKey))


            // We create a transaction builder and add the components.

           // val hashasint: Int =  serviceHub.vaultService.hashCode()
            //val convertostring: String =   hashasint.toString()
            //val ourStateRef = StateRef(SecureHash.sha256(convertostring), 0)
            //val inputState: StateAndRef<KycState> = serviceHub.toStateAndRef(ourStateRef)
        //**************************************************************************
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(KycId))
        val inputStateAndRef = serviceHub.vaultService.queryBy<KycState>(inputCriteria).states.single()
       // val input = inputStateAndRef.state.data
        //********************************************




        //val inputState: StateAndRef<KycState> = serviceHub.toStateAndRef(inputStateAndRef)

//**************************************************************************************

            val txBuilder = TransactionBuilder(notary = notary)
                    .addOutputState(outputState, KycContract.ID)
                    .addCommand(command)
                    .addInputState(inputStateAndRef)



            // val ourStateRef: StateRef = StateRef(SecureHash.sha256(""), 0)



            //txBuilder.addInputState(ourStateAndRef)
            //txBuilder.addAttachment(ourAttachment)


            // Verifying the transaction.
            txBuilder.verify(serviceHub)

            // We sign the transaction.
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Creating a session with the other party.
            val otherPartySession = initiateFlow(otherParty)   // i think here is where the actual link to other node starts
           // subFlow(SendTransactionFlow(otherPartySession,signedTx))
           // subFlow(SendStateAndRefFlow(otherPartySession, )
            // Obtaining the counterparty's signature.
            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

            // We finalise the transaction and then send it to the counterparty.
        return subFlow(FinalityFlow(fullySignedTx, otherPartySession))




        // Initiator flow logic goes here.
    }
}

@InitiatedBy(KycUpdateFlow::class)
 class KycUpdateFlowResponder(  internal val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a KYC transaction." using (output is KycState)

            }
        }


        //subFlow(ReceiveTransactionFlow(otherPartySession, true, StatesToRecord.ONLY_RELEVANT))
        val TxId = subFlow(signTransactionFlow).id
        logger.info("KycUpdateFlowResponder =="+TxId)
        return subFlow(ReceiveFinalityFlow(otherPartySession,TxId))

    }
}


