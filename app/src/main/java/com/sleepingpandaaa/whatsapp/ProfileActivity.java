package com.sleepingpandaaa.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID, sendUserID, Current_State;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestButton, DeclineMessageRequestButton;
    private DatabaseReference UserRef, ChatRequestRef, ContactsRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        mAuth = FirebaseAuth.getInstance();
        sendUserID = mAuth.getCurrentUser().getUid();
        receiverUserID = getIntent().getExtras().get("visit_user_id").toString();

        userProfileImage = findViewById(R.id.visit_profile_image);
        userProfileName = findViewById(R.id.visit_user_name);
        userProfileStatus = findViewById(R.id.visit_profile_status);
        sendMessageRequestButton = findViewById(R.id.send_message_request_button);
        DeclineMessageRequestButton = findViewById(R.id.decline_message_request_button);
        Current_State = "new";

        RetrieveUserInfo();
    }

    private void RetrieveUserInfo()
    {
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.exists()) && (dataSnapshot.hasChild("image")))
                {
                    String userImage = dataSnapshot.child("image").getValue().toString();
                    String userName = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile).into(userProfileImage);
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);


                    ManageChatRequests();

                }
                else {
                    String userName = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequests();   

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void ManageChatRequests()
    {
        ChatRequestRef.child(sendUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(receiverUserID))
                {
                    String request_type = dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();
                    if (request_type.equals("sent"))
                    {
                        Current_State = "request_sent";
                        sendMessageRequestButton.setText("Cancel Chat Request");
                    }
                    else if (request_type.equals("received")){
                        Current_State = "request_received";
                        sendMessageRequestButton.setText("Accept Chat Request");

                        DeclineMessageRequestButton.setVisibility(View.VISIBLE);
                        DeclineMessageRequestButton.setEnabled(true);
                        DeclineMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CancelChatRequest();
                            }
                        });
                    }
                }
                else {
                    ContactsRef.child(sendUserID)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(receiverUserID))
                                    {
                                        Current_State = "friends";
                                        sendMessageRequestButton.setText("Remove Contact");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        if (!sendUserID.equals(receiverUserID))
        {
            sendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessageRequestButton.setEnabled(false);

                    if (Current_State.equals("new"))
                    {
                        SendChatRequest();
                    }
                    if (Current_State.equals("request_sent"))
                    {
                        CancelChatRequest();
                    }if (Current_State.equals("request_received"))
                    {
                        AcceptChatRequest();
                    }if (Current_State.equals("friends"))
                    {
                        RemoveSpecificContact();
                    }
                }
            });
        }
        else {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    private void RemoveSpecificContact()
    {
        ContactsRef.child(sendUserID).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    ContactsRef.child(receiverUserID).child(sendUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                            {
                                sendMessageRequestButton.setEnabled(true);
                                Current_State = "new";
                                sendMessageRequestButton.setText("Send Message");

                                DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                DeclineMessageRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void AcceptChatRequest()
    {
        ContactsRef.child(sendUserID).child(receiverUserID).child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {
                            ContactsRef.child(receiverUserID).child(sendUserID).child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {
                                                ChatRequestRef.child(sendUserID).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        sendMessageRequestButton.setEnabled(true);
                                                        Current_State = "friends";
                                                        sendMessageRequestButton.setText("Remove Contact");

                                                        DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                        DeclineMessageRequestButton.setEnabled(false);
                                                    }
                                                });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void CancelChatRequest()
    {
        ChatRequestRef.child(sendUserID).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    ChatRequestRef.child(receiverUserID).child(sendUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                            {
                                sendMessageRequestButton.setEnabled(true);
                                Current_State = "new";
                                sendMessageRequestButton.setText("Send Message");

                                DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                DeclineMessageRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });


    }

    private void SendChatRequest()
    {
        ChatRequestRef.child(sendUserID).child(receiverUserID).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    ChatRequestRef.child(receiverUserID).child(sendUserID).child("request_type").setValue("received")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful())
                                    {
                                        sendMessageRequestButton.setEnabled(true);
                                        Current_State = "request_sent";
                                        sendMessageRequestButton.setText("Cancel Chat Request");
                                    }
                                }
                            });
                }
            }
        });
    }
}