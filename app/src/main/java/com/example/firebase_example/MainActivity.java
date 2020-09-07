/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.firebase_example;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final int RC_SIGN_IN = 1;

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;
    private FirebaseDatabase mfirebasedatabase;  //Entry point of app to access the database;
    private DatabaseReference mdatabsereference;  //this object reference a specific part of the database here the message part;
    private ChildEventListener mchildeventlistener; //this listener invokes when any new msg is added to the database inside it code to access the msg to the ui
    private FirebaseAuth mfirebaseauth;
    private FirebaseAuth.AuthStateListener mauthstatelistener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
                );


        mUsername = ANONYMOUS;

        mfirebasedatabase = FirebaseDatabase.getInstance(); //creating instance of this class;
        mfirebaseauth = FirebaseAuth.getInstance();
        mdatabsereference = mfirebasedatabase.getReference().child("messages"); //getting refernce to a particular part of the database here messages

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: send messages on click
                //To send messages the object below is created with three parameters
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(),mUsername,null);
                // each message will be added under messages node as its child
                //CLear input box
                mMessageEditText.setText("");
            }
        });



        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mauthstatelistener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user =firebaseAuth.getCurrentUser();
                if(user!=null)
                {
                    Toast.makeText(MainActivity.this, "You are now signed in", Toast.LENGTH_SHORT).show();
                    onSignedInIntialised(user.getDisplayName());
                    //user is signed in
                }
                else
                {
                    //user is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(providers )
                            .build(),RC_SIGN_IN);

                }

            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() //when the activity is no longer in foreground
    {
        super.onPause();
        if(mauthstatelistener != null)
        {
        mfirebaseauth.removeAuthStateListener(mauthstatelistener);}
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @Override
    protected void onResume() //is in the foreground
    {
        super.onResume();
        mfirebaseauth.addAuthStateListener(mauthstatelistener);
    }
    private void onSignedInIntialised(String username)
    {
        mUsername = username; //setting the username
        attachDatabaseReadListener();



    }
    private void onSignedOutCleanup() //this method unsets the username clear msg list and detach listener i.e tear down the ui
    {
        mUsername= ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void detachDatabaseReadListener()
    {
        if(mchildeventlistener != null) {
            mdatabsereference.removeEventListener(mchildeventlistener);
            mchildeventlistener =null;
        }
    }
    private void attachDatabaseReadListener()
    {
        mchildeventlistener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) // called when new message is inserted in the message list
            {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.add(friendlyMessage); //and it will be displayed in our listview

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) // when the content of that msg is changed
            {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) //when the msg is deleted
            {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s)  //when msg change position in the list
            {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) //some error has occurred while changing , here you don't have the permission to read data
            {

            }
        };


        mdatabsereference.addChildEventListener(mchildeventlistener); //this defines what the listener is listening to here mdatabasereference;
        //this also means the listener is just for the messages node only not for the other node than messages
    }
}
