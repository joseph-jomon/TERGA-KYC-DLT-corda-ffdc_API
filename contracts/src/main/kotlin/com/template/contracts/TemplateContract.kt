package com.template.contracts

import com.template.states.KycState
import net.corda.core.contracts.CommandData
//import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.*
import java.security.PublicKey


// ************
// * Contract *
// ************
class KycContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.KycContract"
    }
    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class IssueKyc : Commands
        class UpdateKyc : Commands
    }




    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        // Used to indicate the transaction's intent.




          fun verifyKycIssue(tx: LedgerTransaction) = requireThat {
             // Constraints on the shape of the transaction.
             "No inputs should be consumed when issuing a KYC." using (tx.inputs.isEmpty())
             "There should be one output state of type KycState." using (tx.outputs.size == 1)

             // Kyc-specific constraints.
             val output = tx.outputsOfType<KycState>().single()
             "The owner and the bank cannot be the same entity." using (output.owner != output.bank)

             // Constraints on the signers.
             val expectedSigners = listOf(output.owner.owningKey, output.bank.owningKey)
             "There must be two signers." using (command.signers.toSet().size == 2)
             "The owner and bank must be signers." using (command.signers.containsAll(expectedSigners))

        }

        //********************************************************
        fun verifyKycUpdate(tx: LedgerTransaction) = requireThat {

            // Constraints on the shape of the transaction.
            val input = tx.inputsOfType<KycState>().single()
            "A Kyc update transaction should only consume one input state." using (tx.inputs.size == 1)
            "There should be one output state of type KycState." using (tx.outputs.size == 1)
            //val ourOtherOutputState: KycState = ourOutputState.copy(magicNumber = 77)

            // Kyc-specific constraints.
            val output = tx.outputsOfType<KycState>().single()
            "The owner and the bank cannot be the same entity." using (output.owner != output.bank)

            // Constraints on the signers.

            val expectedSigners = listOf(output.owner.owningKey, output.bank.owningKey)
            "There must be two signers." using (command.signers.toSet().size == 2)
            "The owner and bank must be signers." using (command.signers.containsAll(expectedSigners))

        }
        //**************************************************
        when (command.value) {
            is Commands.IssueKyc -> verifyKycIssue(tx)
            is Commands.UpdateKyc -> verifyKycUpdate(tx)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }






    }


}