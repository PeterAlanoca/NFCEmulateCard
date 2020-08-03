package com.proingesistinrfor.nfcemulatecard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.proingesistinrfor.nfcemulatecard.emulated.HostService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText editTextData;
    private Button buttonSend;
    private NfcAdapter mNfcAdapter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextData = findViewById(R.id.editTextData);
        buttonSend = findViewById(R.id.buttonSend);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }

    @Override
    public void onClick(View view) {
        if (TextUtils.isEmpty(editTextData.getText())) {
            Toast.makeText(this, getString(R.string.app_error_enter_text), Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(this, HostService.class);
            intent.putExtra("ndefMessage", editTextData.getText().toString());
            startService(intent);
        }
    }

    private void initNFCFunction() {

        if (checkNFCEnable() && getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            /*textView.visibility = View.GONE
            editText.visibility = View.VISIBLE
            button.visibility = View.VISIBLE*/
            initService();
        } else {
            /*textView.visibility = View.VISIBLE
            editText.visibility = View.GONE
            button.visibility = View.GONE*/
            //showTurnOnNfcDialog();
        }
    }

    private void initService() {
        buttonSend.setOnClickListener(this);
    }

    private boolean checkNFCEnable() {
        /*return if (mNfcAdapter == null) {
            textView.text = getString(R.string.tv_noNfc)
            false
        } else {
            mNfcAdapter!!.isEnabled
        }*/
        return true;
    }


}