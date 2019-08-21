package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.KycContract
import com.template.states.KycState
import net.corda.core.contracts.Command
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
class KycIssueFlow(val kname: String = "not_shared",val kaddress: String = "not_shared",val kdob: String = "not_shared",val kemail: String = "not_shared", val otherParty: Party ) : FlowLogic<SignedTransaction>() {


    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // We create the transaction components.
        val outputState = KycState(kname,kaddress,kdob,kemail, ourIdentity, otherParty)

            val command = Command(KycContract.Commands.IssueKyc(), listOf(ourIdentity.owningKey, otherParty.owningKey))
            //override fun hashCode(): Int = otherParty.owningKey.hashCode()


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
        return subFlow(FinalityFlow(fullySignedTx, otherPartySession))
    }



        // Initiator flow logic goes here.

}

@InitiatedBy(KycIssueFlow::class)
 class KycIssueFlowResponder(  internal val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a KYC transaction." using (output is KycState)

            }
        }



        val TxId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(otherPartySession,TxId))

    }
}


