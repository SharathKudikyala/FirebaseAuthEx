package com.example.sharath.firebaseauthex;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = MainActivity.class.getSimpleName();
    private Button btnRegister;
    private EditText edtPhoneNo;
    private TextView txtStatus;
    private String mVerificationId;
    private FirebaseAuth mAuth;

    public static void hideSoftKeyboard(Activity activity) {
        if (activity != null && activity.getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        btnRegister = findViewById(R.id.btnRegister);
        edtPhoneNo = findViewById(R.id.edtPhoneNo);
        txtStatus = findViewById(R.id.txtStatus);

        btnRegister.setOnClickListener(this);
        if (mAuth.getCurrentUser() != null)
            updateStatus(mAuth.getCurrentUser().getPhoneNumber());
    }

    private void showOTPPopUp() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom_dialog_otp);
        Button btnProceed = dialog.findViewById(R.id.btnProceed);
        final EditText edtOTP = dialog.findViewById(R.id.edtOTP);
        dialog.setTitle("OTP");
        dialog.setCancelable(false);
        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtOTP.setError(null);
                String otp = edtOTP.getText().toString().trim();
                if (TextUtils.isEmpty(otp) || otp.length() != 6) {
                    edtOTP.setError("Invalid OTP");
                    updateStatus("Invalid OTP");
                    return;
                }
                verifyPhoneNumberWithCode(mVerificationId, otp);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        mAuth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            updateStatus("Success");
                            FirebaseUser user = task.getResult().getUser();
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                updateStatus("The verification code entered was invalid");
                            }
                        }
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        hideSoftKeyboard(MainActivity.this);
        String phoneNo = edtPhoneNo.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNo) || phoneNo.length() != 10) {
            updateStatus("Invalid phone number");
            return;
        }
        edtPhoneNo.setText("");
        updateStatus("Valid Phone number");
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                String.format("+91%s", phoneNo),        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        Log.d(TAG, "onVerificationCompleted:" + phoneAuthCredential);
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.w(TAG, "onVerificationFailed", e);
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            updateStatus("Invalid request");
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            updateStatus("The SMS quota for the project has been exceeded");
                        } else updateStatus(e.getMessage());

                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        updateStatus("Verification code sent");
                        Log.d(TAG, "onCodeSent:" + verificationId);
                        mVerificationId = verificationId;
                        showOTPPopUp();
                    }
                });
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void updateStatus(String status) {
        Snackbar.make(findViewById(android.R.id.content), status, Snackbar.LENGTH_INDEFINITE).show();
        txtStatus.setText(String.format("%s --> %s", txtStatus.getText().toString(), status));
    }
}
