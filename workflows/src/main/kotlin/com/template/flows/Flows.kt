package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KycContract
import com.template.states.KycState
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class KycFlow(val kname: String = "not_shared",val kaddress: String = "not_shared",val kdob: String = "not_shared",val kemail: String = "not_shared", val otherParty: Party,val Type: CommandData ) : FlowLogic<Unit>() {


    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We create the transaction components.
        val outputState = KycState(kname,kaddress,kdob,kemail, ourIdentity, otherParty)
        val command = Command(Type, listOf(ourIdentity.owningKey, otherParty.owningKey))

        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, KycContract.ID)
                .addCommand(command)
        // Verifying the transaction.
        txBuilder.verify(serviceHub)

        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Creating a session with the other party.
        val otherPartySession = initiateFlow(otherParty)

        // Obtaining the counterparty's signature.
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        // We finalise the transaction and then send it to the counterparty.
        subFlow(FinalityFlow(fullySignedTx, otherPartySession))


        // Initiator flow logic goes here.
    }
}

@InitiatedBy(KycFlow::class)
class KycFlowResponder(  val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction." using (output is KycState)

            }
        }

        val expectedTxId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(otherPartySession))

    }
}
