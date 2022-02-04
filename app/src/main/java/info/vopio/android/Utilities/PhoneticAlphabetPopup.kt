package info.vopio.android.Utilities

import android.content.Context
import androidx.appcompat.app.AlertDialog

class PhoneticAlphabetPopup() {

    fun showPopup(viewContext: Context, codeIs: String){

        val characters = codeIs.map {
            it
        }.toTypedArray()

        var phoneticString = codeIs
        phoneticString += "\n\n----------------------------------"

        for (charItem in characters){
            when(charItem.code){
                45 -> phoneticString += "\n\n minus sign"

                48 -> phoneticString += "\n\n number zero"
                49 -> phoneticString += "\n\n number 1"
                50 -> phoneticString += "\n\n number 2"
                51 -> phoneticString += "\n\n number 3"
                52 -> phoneticString += "\n\n number 4"
                53 -> phoneticString += "\n\n number 5"
                54 -> phoneticString += "\n\n number 6"
                55 -> phoneticString += "\n\n number 7"
                56 -> phoneticString += "\n\n number 8"
                57 -> phoneticString += "\n\n number 9"

                65 -> phoneticString += "\n\n Uppercase A"
                66 -> phoneticString += "\n\n Uppercase B"
                67 -> phoneticString += "\n\n Uppercase C"
                68 -> phoneticString += "\n\n Uppercase D"
                69 -> phoneticString += "\n\n Uppercase E"
                70 -> phoneticString += "\n\n Uppercase F"
                71 -> phoneticString += "\n\n Uppercase G"
                72 -> phoneticString += "\n\n Uppercase H"
                73 -> phoneticString += "\n\n Uppercase i"
                74 -> phoneticString += "\n\n Uppercase J"
                75 -> phoneticString += "\n\n Uppercase K"
                76 -> phoneticString += "\n\n Uppercase L"
                77 -> phoneticString += "\n\n Uppercase M"
                78 -> phoneticString += "\n\n Uppercase N"
                79 -> phoneticString += "\n\n Uppercase oh"
                80 -> phoneticString += "\n\n Uppercase P"
                81 -> phoneticString += "\n\n Uppercase Q"
                82 -> phoneticString += "\n\n Uppercase R"
                83 -> phoneticString += "\n\n Uppercase S"
                84 -> phoneticString += "\n\n Uppercase T"
                85 -> phoneticString += "\n\n Uppercase U"
                86 -> phoneticString += "\n\n Uppercase V"
                87 -> phoneticString += "\n\n Uppercase W"
                88 -> phoneticString += "\n\n Uppercase X"
                89 -> phoneticString += "\n\n Uppercase Y"
                90 -> phoneticString += "\n\n Uppercase Z"

                95 -> phoneticString += "\n\n underscore"

                97 -> phoneticString += "\n\n lowercase A"
                98 -> phoneticString += "\n\n lowercase B"
                99 -> phoneticString += "\n\n lowercase C"
                100 -> phoneticString += "\n\n lowercase D"
                101 -> phoneticString += "\n\n lowercase E"
                102 -> phoneticString += "\n\n lowercase F"
                103 -> phoneticString += "\n\n lowercase G"
                104 -> phoneticString += "\n\n lowercase H"
                105 -> phoneticString += "\n\n lowercase i"
                106 -> phoneticString += "\n\n lowercase J"
                107 -> phoneticString += "\n\n lowercase K"
                108 -> phoneticString += "\n\n lowercase L"
                109 -> phoneticString += "\n\n lowercase M"
                110 -> phoneticString += "\n\n lowercase N"
                111 -> phoneticString += "\n\n lowercase oh"
                112 -> phoneticString += "\n\n lowercase P"
                113 -> phoneticString += "\n\n lowercase Q"
                114 -> phoneticString += "\n\n lowercase R"
                115 -> phoneticString += "\n\n lowercase S"
                116 -> phoneticString += "\n\n lowercase T"
                117 -> phoneticString += "\n\n lowercase U"
                118 -> phoneticString += "\n\n lowercase V"
                119 -> phoneticString += "\n\n lowercase W"
                120 -> phoneticString += "\n\n lowercase X"
                121 -> phoneticString += "\n\n lowercase Y"
                122 -> phoneticString += "\n\n lowercase Z"

                else -> phoneticString += "\n\n$charItem is $charItem"
            }
        }

        val alertDialogBuilder = AlertDialog.Builder(viewContext)
        alertDialogBuilder
            .setTitle("Code Breakdown")
            .setMessage(phoneticString)
            .setCancelable(true)
            .setPositiveButton("Got it") { dialog, which ->
                dialog.cancel()
            }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }

}