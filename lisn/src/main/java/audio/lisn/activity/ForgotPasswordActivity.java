package audio.lisn.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import audio.lisn.R;
import audio.lisn.util.ConnectionDetector;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText _emailText;
    Button _resetButton;
    ConnectionDetector connectionDetector;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Forgot Password");

        connectionDetector = new ConnectionDetector(getApplicationContext());

        _emailText = (EditText) findViewById(R.id.input_email);
        _resetButton = (Button) findViewById(R.id.btn_reset);


        _resetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reset();
            }
        });
        progressDialog = new ProgressDialog(ForgotPasswordActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Updating...");
    }

    private void reset() {
        if (!validate()) {
            onResetFailed();
            return;
        }
        if(connectionDetector.isConnectingToInternet()){
            _resetButton.setEnabled(false);
            progressDialog.show();

        }else{
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                    this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    public void onResetFailed() {
        Toast.makeText(getBaseContext(), "Reset password failed", Toast.LENGTH_LONG).show();

        _resetButton.setEnabled(true);
    }
    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();



        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }



        return valid;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
