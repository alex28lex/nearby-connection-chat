package com.example.mihaylov.nearbyconnections;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
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

    @BindView(R.id.recycler)
    RecyclerView recycler;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_advertising:
                startAdvertising();
                break;
            case R.id.action_discovering:
                startDiscovery();
                break;
        }
        invalidateOptionsMenu();
        return true;
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


    private void startAdvertising() {
        Nearby.Connections.startAdvertising(
                googleApiClient,
                Build.MODEL,
                "my_service_id",
                getConnectionLifecycleCallback(),
                new AdvertisingOptions(Strategy.P2P_STAR))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Log.v("NEARBY", "Advertising");
                                } else {
                                    // We were unable to start advertising.
                                }
                            }
                        });
    }


    private void startDiscovery() {
        Nearby.Connections.startDiscovery(
                googleApiClient,
                Build.MODEL,
                getEndpointDiscoveryCallback(),
                new DiscoveryOptions(Strategy.P2P_STAR))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.v("NEARBY", "Discovering");
                                    // We're discovering!
                                } else {
                                    // We were unable to start discovering.
                                }
                            }
                        });
    }


    private ConnectionLifecycleCallback getConnectionLifecycleCallback() {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(final String endpointId, final ConnectionInfo connectionInfo) {
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

                                        Log.v("NEARBY", new String(payload.asBytes()));

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
                                Nearby.Connections.rejectConnection(googleApiClient, "");
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

            @Override
            public void onConnectionResult(String s, ConnectionResolution result) {
                switch (result.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        // The connection was rejected by one or both sides.
                        break;
                }
            }

            @Override
            public void onDisconnected(String s) {

            }
        };
    }


    private EndpointDiscoveryCallback getEndpointDiscoveryCallback() {
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(
                    String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
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
                                            // We successfully requested a connection. Now both sides
                                            // must accept before the connection is established.
                                        } else {
                                            // Nearby Connections failed to request the connection.
                                        }
                                    }
                                });

            }

            @Override
            public void onEndpointLost(String endpointId) {

            }
        };
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @OnClick(R.id.sendBtn)
    public void onViewClicked() {
        Nearby.Connections.sendReliableMessage(googleApiClient, "qwe", editTextMessage.getText().toString().getBytes(Charset.forName("UTF-8")));
    }
}
