package com.proingesistinrfor.nfcemulatecard.emulated;

import android.app.Service;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

public class HostService extends HostApduService {

    private static final String TAG = "HostApduService";

    private static final byte[] APDU_SELECT = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xA4, // INS	- Instruction - Instruction code
            (byte) 0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x07, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xD2,
            (byte) 0x76,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x85,
            (byte) 0x01,
            (byte) 0x01, // NDEF Tag Application name
            (byte) 0x00  // Le field	- Maximum number of bytes expected in the data field of the response to the command
    };

    private static final byte[] CAPABILITY_CONTAINER_OK = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xe1,
            (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] READ_CAPABILITY_CONTAINER = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xb0, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x0f  // Lc field	- Number of bytes present in the data field of the command
    };

    // In the scenario that we have done a CC read, the same byte[] match
    // for ReadBinary would trigger and we don't want that in succession
    private boolean READ_CAPABILITY_CONTAINER_CHECK = false;

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE = new byte[]{
            (byte) 0x00,
            (byte) 0x11, // CCLEN length of the CC file
            (byte) 0x20, // Mapping Version 2.0
            (byte) 0xFF,
            (byte) 0xFF, // MLe maximum
            (byte) 0xFF,
            (byte) 0xFF, // MLc maximum
            (byte) 0x04, // T field of the NDEF File Control TLV
            (byte) 0x06, // L field of the NDEF File Control TLV
            (byte) 0xE1,
            (byte) 0x04, // File Identifier of NDEF file
            (byte) 0xFF,
            (byte) 0xFE, // Maximum NDEF file size of 65534 bytes
            (byte) 0x00, // Read access without any security
            (byte) 0xFF, // Write access without any security
            (byte) 0x90,
            (byte) 0x00 // A_OKAY
    };

    private static final byte[] NDEF_SELECT_OK = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1,
            (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private static final byte[] NDEF_READ_BINARY = new byte[]{
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0 // Instruction byte (INS) for ReadBinary command
    };

    private static final byte[] NDEF_READ_BINARY_NLEN = new byte[]{
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0, // Instruction byte (INS) for ReadBinary command
            (byte) 0x00,
            (byte) 0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte) 0x02  // Le field
    };

    private static final byte[] A_OKAY = new byte[]{
            (byte) 0x90, // SW1	Status byte 1 - Command processing status
            (byte) 0x00   // SW2	Status byte 2 - Command processing qualifier
    };

    private static final byte[] A_ERROR = new byte[]{
            (byte) 0x6A, // SW1	Status byte 1 - Command processing status
            (byte) 0x82  // SW2	Status byte 2 - Command processing qualifier
    };

    private static final byte[] NDEF_ID = new byte[]{
            (byte) 0xE1,
            (byte) 0x04
    };

    private NdefMessage NDEF_URI = new NdefMessage(createTextRecord("en", "Ciao, come va?", NDEF_ID));

    private byte[] NDEF_URI_BYTES = NDEF_URI.toByteArray();

    private byte[] NDEF_URI_LEN = fillByteArrayToFixedDimension(BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray(), 2);


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra("ndefMessage")) {
            NDEF_URI = new NdefMessage(createTextRecord("en", intent.getStringExtra("ndefMessage"), NDEF_ID));
            NDEF_URI_BYTES = NDEF_URI.toByteArray();
            NDEF_URI_LEN = fillByteArrayToFixedDimension(BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray(), 2);
        }
        Log.i(TAG, "onStartCommand() | NDEF" + NDEF_URI.toString());
        return Service.START_STICKY;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {

        //
        // The following flow is based on Appendix E "Example of Mapping Version 2.0 Command Flow"
        // in the NFC Forum specification
        //
        Log.i(TAG, "processCommandApdu() | incoming commandApdu: " + toHex(commandApdu));

        //
        // First command: NDEF Tag Application select (Section 5.5.2 in NFC Forum spec)
        //
        if (Arrays.equals(APDU_SELECT, commandApdu)) {
            Log.i(TAG, "APDU_SELECT triggered. Our Response: " + toHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Second command: Capability Container select (Section 5.5.3 in NFC Forum spec)
        //
        if (Arrays.equals(CAPABILITY_CONTAINER_OK, commandApdu)) {
            Log.i(TAG, "CAPABILITY_CONTAINER_OK triggered. Our Response: " + toHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Third command: ReadBinary data from CC file (Section 5.5.4 in NFC Forum spec)
        //
        if (Arrays.equals(READ_CAPABILITY_CONTAINER, commandApdu) && !READ_CAPABILITY_CONTAINER_CHECK) {
            Log.i(TAG,"READ_CAPABILITY_CONTAINER triggered. Our Response: " + toHex(READ_CAPABILITY_CONTAINER_RESPONSE));
            READ_CAPABILITY_CONTAINER_CHECK = true;
            return READ_CAPABILITY_CONTAINER_RESPONSE;
        }

        //
        // Fourth command: NDEF Select command (Section 5.5.5 in NFC Forum spec)
        //
        if (Arrays.equals(NDEF_SELECT_OK, commandApdu)) {
            Log.i(TAG, "NDEF_SELECT_OK triggered. Our Response: " + toHex(A_OKAY));
            return A_OKAY;
        }

        if (Arrays.equals(NDEF_READ_BINARY_NLEN, commandApdu)) {
            // Build our response
            byte[] response = new byte[NDEF_URI_LEN.length + A_OKAY.length];

            System.arraycopy(NDEF_URI_LEN, 0, response, 0, NDEF_URI_LEN.length);
            System.arraycopy(A_OKAY, 0, response, NDEF_URI_LEN.length, A_OKAY.length);

            Log.i(TAG, "NDEF_READ_BINARY_NLEN triggered. Our Response: " + toHex(response));

            READ_CAPABILITY_CONTAINER_CHECK = false;
            return response;
        }

        if (Arrays.equals(getSliceOfArray(commandApdu, 0 ,1), NDEF_READ_BINARY)) {

            int offset = Integer.parseInt(toHex(getSliceOfArray(commandApdu, 2 ,3)));

            int length = Integer.parseInt(toHex(getSliceOfArray(commandApdu, 4 ,4)));

            byte[] fullResponse = new byte[NDEF_URI_LEN.length + NDEF_URI_BYTES.length];

            System.arraycopy(NDEF_URI_LEN, 0, fullResponse, 0, NDEF_URI_LEN.length);
            System.arraycopy(NDEF_URI_BYTES,0, fullResponse, NDEF_URI_LEN.length, NDEF_URI_BYTES.length);

            Log.i(TAG, "NDEF_READ_BINARY triggered. Full data: " + toHex(fullResponse));
            Log.i(TAG, "READ_BINARY - OFFSET: " + offset + " - LEN: " + length);

            byte[] slicedResponse = getSliceOfArray(fullResponse, offset ,fullResponse.length);

            // Build our response
            int realLength = slicedResponse.length <= length ? slicedResponse.length : length;

            byte[] response = new byte[realLength + A_OKAY.length];

            System.arraycopy(slicedResponse, 0, response, 0, realLength);
            System.arraycopy(A_OKAY, 0, response, realLength, A_OKAY.length);

            Log.i(TAG, "NDEF_READ_BINARY triggered. Our Response: " + toHex(response));

            READ_CAPABILITY_CONTAINER_CHECK = false;
            return response;
        }

        //
        // We're doing something outside our scope
        //
        Log.wtf(TAG, "processCommandApdu() | I don't know what's going on!!!");
        return A_ERROR;
    }

    @Override
    public void onDeactivated(int i) {
        Log.i(TAG, "onDeactivated() Fired! Reason: $reason");
    }

    private static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_CHARS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] getSliceOfArray(byte[] arr, int start, int end) {

        // Get the slice of the Array
        byte[] slice = new byte[end - start];

        // Copy elements of arr to slice
        for (int i = 0; i < slice.length; i++) {
            slice[i] = arr[start + i];
        }

        // return the slice
        return slice;
    }


    private NdefRecord createTextRecord(String language, String text, byte[] id) {
        byte[] languageBytes = new byte[0];
        byte[] textBytes = new byte[0];

        try {
            languageBytes = language.getBytes("US-ASCII");
            textBytes = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] recordPayload = new byte[1 + languageBytes.length + textBytes.length];
        recordPayload[0] = (byte) languageBytes.length;
        System.arraycopy(languageBytes, 0, recordPayload, 1, languageBytes.length);
        System.arraycopy(textBytes, 0, recordPayload, 1 + languageBytes.length, textBytes.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, id, recordPayload);
    }

    private byte[] fillByteArrayToFixedDimension(byte[] array, int fixedSize) {
        if (array.length == fixedSize) {
            return array;
        }
        byte[] start = new byte[]{ (byte) 0x00 };
        byte[] filledArray = new byte[start.length + array.length];
        System.arraycopy(start, 0, filledArray, 0, start.length);
        System.arraycopy(array, 0, filledArray, start.length, array.length);
        return fillByteArrayToFixedDimension(filledArray, fixedSize);
    }

}
