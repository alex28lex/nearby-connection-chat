package com.example.mihaylov.nearbyconnections;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.charset.Charset;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "NEARBY_PROJECT:";

    // client's name that's visible to other devices when connecting
    public static final String CLIENT_NAME = "CLIENT";

    // host's name that's visible to other devices when connecting
    public static final String HOST_NAME = "HOST";

    private String userName = "";

    public static final Strategy STRATEGY = Strategy.P2P_STAR;

    @BindView(R.id.textField)
    TextView textFiled;
    @BindView(R.id.editTextMessage)
    EditText editTextMessage;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        googleApiClient = new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

    }


    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }


    private void startAdvertise() {
        Nearby.Connections.startAdvertising(
                googleApiClient,
                HOST_NAME,
                getString(R.string.service_id),
                getConnectionLifecycleCallback(),
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Log.i(TAG, "Advertising endpoint");
                                } else {
                                    Log.i(TAG, "unable to start advertising");
                                }
                            }
                        });
    }


    private void startDiscovery() {
        Nearby.Connections.startDiscovery(
                googleApiClient,
                CLIENT_NAME,
                getEndpointDiscoveryCallback(),
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.v(TAG, "Discovering success");
                                    // We're discovering!
                                } else {
                                    // We were unable to start discovering.
                                    Log.i(TAG, "unable to start discovering");
                                }
                            }
                        });
    }


    private ConnectionLifecycleCallback getConnectionLifecycleCallback() {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(final String endpointId, final ConnectionInfo connectionInfo) {
                Log.i(TAG, endpointId + " connection initiated");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                        .setMessage("Confirm if the code " + connectionInfo.getAuthenticationToken() + " is also displayed on the other device")
                        .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The user confirmed, so we can accept the connection.
                                Toast.makeText(MainActivity.this, "Connection with " + connectionInfo.getEndpointName() + "success", Toast.LENGTH_SHORT).show();
                                Nearby.Connections.acceptConnection(googleApiClient, endpointId, new PayloadCallback() {
                                    @Override
                                    public void onPayloadReceived(String s, Payload payload) {

                                        String newMessage = new String(payload.asBytes());
                                        Log.v(TAG, "onPayloadReceived: " + newMessage);
                                        String text = textFiled.getText().toString();
                                        textFiled.setText(text + "\n" + newMessage);

                                    }

                                    @Override
                                    public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

                                    }
                                });
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The user canceled, so we should reject the connection.
                                Nearby.Connections.rejectConnection(googleApiClient, getString(R.string.service_id));
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

            @Override
            public void onConnectionResult(String s, ConnectionResolution result) {
                switch (result.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        Log.i(TAG, " onConnectionResult: connected");
                        // We're connected! Can now start sending and receiving data.
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        // The connection was rejected by one or both sides.
                        Log.i(TAG, " onConnectionResult: rejected");
                        break;
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.i(TAG, endpointId + " disconnected");
            }
        };
    }


    private EndpointDiscoveryCallback getEndpointDiscoveryCallback() {
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.i(TAG, endpointId + " endpoint found");
                // An endpoint was found!
                Nearby.Connections.requestConnection(
                        googleApiClient,
                        Build.MODEL,
                        endpointId,
                        getConnectionLifecycleCallback())
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        if (status.isSuccess()) {
                                            Log.i(TAG, "successfully requested a connection");
                                            // We successfully requested a connection. Now both sides
                                            // must accept before the connection is established.
                                        } else {
                                            // Nearby Connections failed to request the connection.
                                            Log.i(TAG, "failed requested a connection");
                                        }
                                    }
                                });

            }

            @Override
            public void onEndpointLost(String endpointId) {
                Log.i(TAG, endpointId + " endpoint lost");
            }
        };
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, " onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, " onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, " onConnectionFailed");
    }


    @OnClick({R.id.disconnectBtn, R.id.ClientBtn, R.id.HostBtn, R.id.sendBtn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.disconnectBtn:
                Log.i(TAG, " onDisconnectClicked");
                Nearby.Connections.stopAdvertising(googleApiClient);
                Nearby.Connections.stopDiscovery(googleApiClient, getString(R.string.service_id));
                break;
            case R.id.ClientBtn:
                startDiscovery();
                break;
            case R.id.HostBtn:
                startAdvertise();
                break;
            case R.id.sendBtn:
                Nearby.Connections.sendReliableMessage(googleApiClient, userName, (userName + ":" + editTextMessage.getText().toString()).getBytes(Charset.forName("UTF-8")));
                break;
        }
    }
}
