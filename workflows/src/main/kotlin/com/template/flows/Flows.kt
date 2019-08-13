package com.template.flows
import  com.template.states.KycState

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.contracts.Command
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty







// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class KycFlow(val kname: String ,val kaddress: String,val kdob: String,val kemail: String, val otherParty: Party ) : FlowLogic<Unit>() {


    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We create the transaction components.
        val outputState = KycState(kname,kaddress,kdob,kemail, ourIdentity, otherParty)
        val command = Command(TemplateContract.Commands.Action(), ourIdentity.owningKey)

        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command)
        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        // Creating a session with the other party.
        val otherPartySession = initiateFlow(otherParty)
        // We finalise the transaction and then send it to the counterparty.
        subFlow(FinalityFlow(signedTx, otherPartySession))


        // Initiator flow logic goes here.
    }
}

@InitiatedBy(KycFlow::class)
class KycFlowResponder(  val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}
