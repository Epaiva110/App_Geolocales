package pe.gob.minam.recycleapp.view.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.realm.Realm;
import pe.gob.minam.recycleapp.R;
import pe.gob.minam.recycleapp.adapter.ClusterRendererPuntosInteres;
import pe.gob.minam.recycleapp.adapter.DirectionsJSONParser;
import pe.gob.minam.recycleapp.adapter.ListViewPuntosInteresAdapter;
import pe.gob.minam.recycleapp.adapter.MapWrapperLayout;
import pe.gob.minam.recycleapp.adapter.MarkerReciclaje;
import pe.gob.minam.recycleapp.adapter.OnInfoWindowElemTouchListener;
import pe.gob.minam.recycleapp.data.model.PuntoInteres;
import pe.gob.minam.recycleapp.data.model.Ubicacion;
import pe.gob.minam.recycleapp.data.remote.APIRestful;
import pe.gob.minam.recycleapp.util.ConstanstRecycle;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        View.OnClickListener {

    private FloatingActionButton fabFilter, fabMap, fabList, fabcenter;
    private RecyclerView reyclerViewListaPuntos;
    private SweetAlertDialog progress;
    private TextView infoTitle, infoDireccion, infoContacto, infoCelularCorreo,
            mSegregacionTitulo, mSegregacionDetalle;
    private ViewGroup infoWindow;
    private Button infoButton;
    private OnInfoWindowElemTouchListener infoButtonListener;
    private LinearLayout layoutSegregacion;

    private boolean mapaIniciado = false, procesoDescarga = false, hideBotones = true;
    private int contadorDescarga, totalDescarga, contadorScreen = 0;
    private boolean listPuntosDownload = false;
    public static String idResiduo, nameTipoResiduo;

    //private SupportMapFragment mapFragment;
    private MapView mapPuntosView;
    private MapWrapperLayout mapWrapperLayout;
    private GoogleMap mapa;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Ubicacion locUbicacion = null;

    private Polyline polyline;

    private ClusterManager<MarkerReciclaje> miMarcadorClusterManager;
    private List<PuntoInteres> puntoInteresList;
    private PuntoInteres segregacion = null;

    private Realm realm;

    public MapFragment() {
        // Required empty public constructor
    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        realm = Realm.getDefaultInstance();

        Intent intent = getActivity().getIntent();
        idResiduo = intent.getStringExtra("idTipoResiduo");
        nameTipoResiduo = intent.getStringExtra("descTipoResiduo");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        locUbicacion = new Ubicacion();

        fabFilter = (FloatingActionButton) view.findViewById(R.id.filter);
        fabMap = (FloatingActionButton) view.findViewById(R.id.mapPuntos);
        fabList = (FloatingActionButton) view.findViewById(R.id.listPuntos);
        fabcenter = (FloatingActionButton) view.findViewById(R.id.fabcenter);
        reyclerViewListaPuntos = (RecyclerView) view.findViewById(R.id.lvPointsRecycle);

        mSegregacionTitulo = (TextView) view.findViewById(R.id.mSegregacionTitulo);
        mSegregacionDetalle = (TextView) view.findViewById(R.id.mSegregacionDetalle);
        /*btnBackHome = (ImageView) findViewById(R.id.btnBackHome);
        btnSettings = (ImageView) findViewById(R.id.btnSettings);
        btnCenter = (ImageView) findViewById(R.id.btnCenter);*/

        layoutSegregacion = (LinearLayout) view.findViewById(R.id.layoutSegregacion);

        /*segUp = (ImageView) findViewById(R.id.segUp);
        segDown = (ImageView) findViewById(R.id.segDown);*/

        MapsInitializer.initialize(this.getActivity());
        mapPuntosView = (MapView) view.findViewById(R.id.mapPuntosView);
        //mapFragment = (SupportMapFragment) getFragmentManager().findFragmentById(R.id.supportMapFragment);
        mapWrapperLayout = (MapWrapperLayout) view.findViewById(R.id.mapRelativeLayout);
        mapPuntosView.onCreate(savedInstanceState);
        mapPuntosView.getMapAsync(this);
        //mapFragment.getMapAsync(this);

        GridLayoutManager gridlm = new GridLayoutManager(getContext(), 1);
        reyclerViewListaPuntos.setHasFixedSize(true);
        reyclerViewListaPuntos.setLayoutManager(gridlm);
        reyclerViewListaPuntos.setVisibility(View.INVISIBLE);

        fabMap.setVisibility(View.INVISIBLE);
        fabcenter.setVisibility(View.INVISIBLE);
        //viewSegregacion();

        fabFilter.setOnClickListener(this);
        fabMap.setOnClickListener(this);
        fabList.setOnClickListener(this);
        fabcenter.setOnClickListener(this);
        mSegregacionDetalle.setOnClickListener(this);

        return view;
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("AMVA", "Localización: " + location.getLatitude() + " " + location.getLongitude());
                lastLocation = location;
                if(lastLocation!=null && !listPuntosDownload){
                    //traer data
                    listPuntosDownload = true;
                    centerLocation();
                }
                //ver la forma de descargar puntos mientras se avanza
                try{
                    if(segregacion.getPunto()==null){
                        APIRestful.getJsonSegregacion(idResiduo,
                                locUbicacion.getLatitud().toString(),
                                locUbicacion.getLongitud().toString(), "", "", "", getActivity());

                    }else{
                        Log.d("AMVA", "Data segregacion realm " + realm.where(PuntoInteres.class).findFirst().getPunto());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

                if(mSegregacionTitulo.getText().equals("Cargando ...")){
                    int secondsDelayed = 2;
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            validateSegregacion();
                        }
                    }, secondsDelayed * 1000);
                }else{
                    Log.d("AMVA", "segregacion tiene contenido");
                }
            }
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View v) {
        if( v == fabFilter ){
            FragmentManager fm = this.getChildFragmentManager();
            EditPreferenciaDialog editNameDialogFragment = EditPreferenciaDialog.newInstance("Lugar de preferencia");
            editNameDialogFragment.setOnUpdateUbigeo(new EditPreferenciaDialog.OnUpdateUbigeo() {
                @Override
                public void updatePuntosInteres(final List<PuntoInteres> puntosListUpdate, final String departamento, final String provincia, final String distrito) {

                    int secondsDelayed = 3;
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            if(puntosListUpdate.size()>0){
                                Log.d("XXXX","Ejecuta interface " + puntosListUpdate.toString());
                                getDataMap(puntosListUpdate);
                                moveCamera(puntosListUpdate);
                                APIRestful.getJsonSegregacion(idResiduo,"", "", departamento, provincia, distrito, getActivity());
                                int secondsDelayed = 2;
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        validateSegregacion();
                                    }
                                }, secondsDelayed * 1000);
                            }else{
                                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                        .setTitle("Mensaje")
                                        .setMessage("No se encontró resultados.")
                                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .create();
                                dialog.show();
                            }
                        }
                    }, secondsDelayed * 1000);

                }
            });
            editNameDialogFragment.show(fm, "fragment_edit_preferencia");
            editNameDialogFragment.getReturnTransition();
        }else if( v == fabMap ){
            fabList.setVisibility(View.VISIBLE);
            fabMap.setVisibility(View.INVISIBLE);
            mapPuntosView.setVisibility(View.VISIBLE);
            fabFilter.setVisibility(View.VISIBLE);
            reyclerViewListaPuntos.setVisibility(View.INVISIBLE);
            layoutSegregacion.setVisibility(View.VISIBLE);
        }else if( v == fabList ){
            fabMap.setVisibility(View.VISIBLE);
            fabList.setVisibility(View.INVISIBLE);
            reyclerViewListaPuntos.setVisibility(View.VISIBLE);
            mapPuntosView.setVisibility(View.INVISIBLE);
            fabFilter.setVisibility(View.INVISIBLE);
            layoutSegregacion.setVisibility(View.GONE);
        }else if( v == fabcenter ){
            centerLocation();
            viewButtonMap();
        }else if( v == mSegregacionDetalle ){
            onClickLlamada(mSegregacionDetalle.getText().toString());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!checkPlayServices()) {
            Toast.makeText(getActivity(), "Necesita instalar Google Play Services.", Toast.LENGTH_LONG).show();
        }
        if (googleApiClient != null && fusedLocationProviderClient != null) {
            startLocationUpdates();
        } else {
            buildGoogleApiClient();
        }
        mapPuntosView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // stop location updates
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        //stop location updates when Activity is no longer active
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        mapPuntosView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // stop location updates
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        //stop location updates when Activity is no longer active
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        mapPuntosView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState); mapPuntosView.onSaveInstanceState(outState);
    }
    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mapPuntosView.onLowMemory();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void onClickLlamada(String telefono) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", telefono, null));
        startActivity(intent);
        /*Intent i = new Intent(Intent.ACTION_CALL);
        i.setData(Uri.parse(telefono));
        startActivity(i);*/
    }

    @SuppressLint("RestrictedApi")
    public void viewButtonMap()
    {
        hideBotones=false;
        if (layoutSegregacion.getVisibility() == View.GONE){
            //animar(true);
            layoutSegregacion.setVisibility(View.VISIBLE);
        }
        if (fabFilter.getVisibility() == View.GONE){
            fabFilter.setVisibility(View.VISIBLE);
        }
        if (fabList.getVisibility() == View.GONE){
            fabList.setVisibility(View.VISIBLE);
        }
        if (fabcenter.getVisibility() == View.VISIBLE){
            fabcenter.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressLint("RestrictedApi")
    public void hideButtonMap()
    {
        hideBotones=true;
        contadorScreen=0;
        if (layoutSegregacion.getVisibility() == View.VISIBLE){
            //animar(true);
            layoutSegregacion.setVisibility(View.GONE);
        }
        if (fabFilter.getVisibility() == View.VISIBLE){
            fabFilter.setVisibility(View.GONE);
        }
        if (fabList.getVisibility() == View.VISIBLE){
            fabList.setVisibility(View.GONE);
        }
        if (fabcenter.getVisibility() == View.INVISIBLE){
            fabcenter.setVisibility(View.VISIBLE);
        }
    }

    /*private void animar(boolean mostrar)
    {
        AnimationSet set = new AnimationSet(true);
        Animation animation = null;
        if (mostrar)
        {
            //desde la esquina inferior derecha a la superior izquierda
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        }
        else
        {    //desde la esquina superior izquierda a la esquina inferior derecha
            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        }
        //duración en milisegundos
        animation.setDuration(500);
        set.addAnimation(animation);
        LayoutAnimationController controller = new LayoutAnimationController(set, 0.25f);

        layoutSegregacion.setLayoutAnimation(controller);
        layoutSegregacion.startAnimation(animation);
    }*/

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(getActivity(), resultCode, ConstanstRecycle.PuntosMap.PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                //finish();
                Log.d("AMVA", "Error en checkPlayServices");
            }
            return false;
        }
        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//PRIORITY_BALANCED_POWER_ACCURACY
        locationRequest.setInterval(ConstanstRecycle.PuntosMap.UPDATE_INTERVAL);//tiempo de consulta
        locationRequest.setFastestInterval(ConstanstRecycle.PuntosMap.FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else {
            if(!isLocationEnabled()){
                showAlert();
            }
            //Toast.makeText(this, "You need to enable permissions to display location!", Toast.LENGTH_SHORT).show();
        }
        centerLocation();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("Activar Ubicación")
                .setMessage("\n" +
                        "La configuración de su ubicación está como 'Desactivado'.\nPor favor activar su ubicación para " +
                        "usar esta app")
                .setPositiveButton("Configuración de Ubicación", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        //dismiss;
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    private void centerLocation() {
        if (mapa != null) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if(!isLocationEnabled()){
                    showAlert();
                }
                return;
            }
            //mapa.getUiSettings().setMapToolbarEnabled(false);
            //mapa.getUiSettings().setZoomControlsEnabled(false);
            mapa.getUiSettings().setMyLocationButtonEnabled(false);
            mapa.setMyLocationEnabled(true);
            //Centrar mapa según ubicación guardada o por defecto
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                lastLocation = task.getResult();
                                /*lat_actual = lastLocation.getLatitude();
                                lng_actual = lastLocation.getLongitude();*/

                                if(lastLocation!=null){
                                    mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), ConstanstRecycle.PuntosMap.MAP_ZOOM));
                                    locUbicacion.setLatitud(lastLocation.getLatitude());
                                    locUbicacion.setLongitud(lastLocation.getLongitude());
                                    APIRestful.getJsonSegregacion(idResiduo,
                                            locUbicacion.getLatitud().toString(),
                                            locUbicacion.getLongitud().toString(), "", "", "", getActivity());
                                    getPuntosInteres();
                                    if (!mapaIniciado) {
                                        mapaIniciado = true;
                                        getDataMap(puntoInteresList);
                                        //moveCamera(puntoInteresList);
                                    }
                                }else{
                                    showAlert();
                                }
                            } else {
                                showAlert();
                            }
                        }
                    });
        }
    }

    private void getPuntosInteres() {
        puntoInteresList = APIRestful.getJsonPuntoInteres(idResiduo,
                locUbicacion.getLatitud().toString(),
                locUbicacion.getLongitud().toString(), "", "", "", getActivity());
    }

    private List<PuntoInteres> dataForDistanceDuration(List<PuntoInteres> interesList){
        String destinos = "";
        int contador = 0;
        for(PuntoInteres puntodestino : interesList){
            contador++;
            destinos = destinos + puntodestino.getLatitud() + "," + puntodestino.getLongitud();
            if(contador<interesList.size()){
                destinos = destinos + "|";
            }
        }
        String latLngString = locUbicacion.getLatitud() + "," + locUbicacion.getLongitud();
        return APIRestful.getResultDistance(latLngString, destinos, interesList);
    }

    public void getDataMap(final List<PuntoInteres> list) {
        //if (!mapaIniciado) {
            //mapaIniciado = true;
            //getPuntosInteres();
            progress = new SweetAlertDialog(getContext(), SweetAlertDialog.PROGRESS_TYPE);
            progress.getProgressHelper().setBarColor(Color.parseColor("#23506e"));
            progress.setTitleText("Sincronizando ...");
            progress.setCancelable(false);
            progress.show();

            int secondsDelayed = 3;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    List<PuntoInteres> ptInteresList = null;
                    if (dataForDistanceDuration(list).size()>0){
                        ptInteresList = dataForDistanceDuration(list);
                        Log.d("XXXX","Data for duration");
                    }else{
                        ptInteresList = list;
                        Log.d("XXXX","Misma lista " + list.toString());
                    }

                    addPuntosInteres(list);

                    int secondsDelayed = 2;
                    final List<PuntoInteres> finalPtInteresList = ptInteresList;
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            ListViewPuntosInteresAdapter mAdapter = new ListViewPuntosInteresAdapter(finalPtInteresList);
                            reyclerViewListaPuntos.setAdapter(mAdapter);
                        }
                    },secondsDelayed * 1000);

                    getDistances();
                    progress.dismiss();
                    progress = null;
                }
            }, secondsDelayed * 1000);

            miMarcadorClusterManager = new ClusterManager<>(getContext(), mapa);
            mapa.setOnCameraIdleListener(miMarcadorClusterManager);
            mapa.setOnMarkerClickListener(miMarcadorClusterManager);
            miMarcadorClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MarkerReciclaje>() {
                @Override
                public boolean onClusterItemClick(final MarkerReciclaje miMarcador) {
                    if(miMarcador!=null){
                        //
                        final GoogleMap googleMap = mapa;
                        if(googleMap != null){
                            mapWrapperLayout.init(googleMap, getPixelsFromDp(39 + 20));
                            infoWindowsData(miMarcador);
                            mapa.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                                @Override
                                public View getInfoWindow(Marker marker) {
                                    infoTitle.setText(miMarcador.getPuntoReciclaje().getPunto());
                                    String texto = miMarcador.getPuntoReciclaje().getEntidad() + "\n" + miMarcador.getPuntoReciclaje().getDireccion();
                                    infoDireccion.setText(texto);
                                    /*infoContacto.setText(miMarcador.getPuntoReciclaje().getContacto());
                                    infoCelularCorreo.setText(miMarcador.getPuntoReciclaje().getCelular() + " - " + miMarcador.getPuntoReciclaje().getEmail());*/
                                    infoButtonListener.setMarker(marker);

                                    // We must call this to set the current marker and infoWindow references
                                    // to the MapWrapperLayout
                                    mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);
                                    return infoWindow;
                                }

                                @Override
                                public View getInfoContents(Marker marker) {
                                    return null;
                                }
                            });
                        }

                    }
                    return false;
                }
            });

            miMarcadorClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MarkerReciclaje>() {
                @Override
                public boolean onClusterClick(Cluster<MarkerReciclaje> cluster) {
                    mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            cluster.getPosition(), (float) Math.floor(mapa
                                    .getCameraPosition().zoom + 2)), 300,
                            null);
                    return true;
                }
            });
        //}
    }

    public int getPixelsFromDp(float dp) {

        final float scale = getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    public void validateSegregacion(){
        segregacion = realm.where(PuntoInteres.class).findFirst();
        if(segregacion != null ) {
            String detalle = "";

            if (!segregacion.getCelular().equalsIgnoreCase("")){
                detalle = detalle + segregacion.getCelular().trim() + "\n";
            }
            /*if (!segregacion.getHorario().equalsIgnoreCase("")){
                detalle = detalle + segregacion.getHorario().trim() ;
            }
            if (!segregacion.getDireccion().equalsIgnoreCase("")){
                detalle = detalle + segregacion.getDireccion().trim() + "\n";
            }*/
            mSegregacionTitulo.setText(segregacion.getEntidad());
            mSegregacionDetalle.setText(detalle);

            //Se comenta porque segregación no se agrega como punto de interés en el mapa
            //miMarcadorClusterManager.addItem(new MarkerReciclaje(segregacion));
        }
    }

    private void addPuntosInteres(List<PuntoInteres> listPuntos) {
        /*if(!procesoDescarga && mapaIniciado){
            procesoDescarga = true;*/
            contadorDescarga = 0;
            totalDescarga = 0;

            mapa.clear();
            miMarcadorClusterManager.clearItems();//eliminar todos los puntos
            for (PuntoInteres puntoInteres : listPuntos) {
                miMarcadorClusterManager.addItem(new MarkerReciclaje(puntoInteres));
                contadorDescarga++;
            }
            try{
                Log.d("AMVA", "Segregacion " + segregacion.toString());
                validateSegregacion();
            }catch (Exception e){e.printStackTrace();}

            final ClusterRendererPuntosInteres renderer = new ClusterRendererPuntosInteres(getContext(), mapa, miMarcadorClusterManager);

            miMarcadorClusterManager.setRenderer(renderer);
        /*}else{
            Log.d("AMVA", "NO INGRESA");
        }*/
    }

    public void moveCamera(List<PuntoInteres> list){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (PuntoInteres puntoInteres : list) {
            builder.include(new LatLng(Double.valueOf(puntoInteres.getLatitud()),Double.valueOf(puntoInteres.getLongitud())));
        }
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mapa.animateCamera(cu);
    }

    public void infoWindowsData(final MarkerReciclaje punto){
        infoWindow = (ViewGroup)getLayoutInflater().inflate(R.layout.windows_info, null);
        infoTitle = (TextView)infoWindow.findViewById(R.id.txtIwTitle);
        infoDireccion = (TextView)infoWindow.findViewById(R.id.txtIwDirection);
        /*infoContacto = (TextView)infoWindow.findViewById(R.id.txtIwContacto);
        infoCelularCorreo = (TextView)infoWindow.findViewById(R.id.txtIwCelCorreo);*/
        infoButton = (Button)infoWindow.findViewById(R.id.btnIwGo);

        infoButtonListener = new OnInfoWindowElemTouchListener(infoButton)
        {
            @Override
            protected void onClickConfirmed(View v, Marker marker) {
                clearPath();
                drawRoute(punto);
            }
        };
        this.infoButton.setOnTouchListener(infoButtonListener);
    }

    private void drawRoute(MarkerReciclaje destination){
        try{
            LatLng origin = new LatLng(locUbicacion.getLatitud(), locUbicacion.getLongitud());
            LatLng dest = destination.getPosition();

            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getActivity(), "No se pudo consultar la ruta.", Toast.LENGTH_LONG).show();
        }
    }

    private void clearPath() {
        if (polyline != null)
            polyline.remove();
        polyline = null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setMapToolbarEnabled(false);//Oculta opcion para ir a Navegador GoogleMap
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mapa.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getContext(), R.raw.style_json));
            if (!success) {
                Log.e("AMVA", "El análisis del estilo ha fallado.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("AMVA", "No se puede encontrar el estilo. Error: ", e);
        }

        if (googleApiClient != null && fusedLocationProviderClient != null) {
            startLocationUpdates();
        }

        mapa.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("AMVA", "onMapClick");
                animation(hideBotones);
            }
        });

        mapa.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    Log.d("AMVA", "onCameraMoveStarted");
                    animation(hideBotones);
                }
            }
        });

        mapa.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d("AMVA", "onMarkerClick");
                animation(hideBotones);
                return true;
            }
        });

    }

    private Handler myHandler = new Handler();
    private Runnable myRunnable = new Runnable() {
        public void run() {
            viewButtonMap();
            hideBotones = false;
        }
    };

    public void animation(boolean interaction){
        hideButtonMap();
        int secondsDelayed = 2;
        if(interaction){
            //Reiniciar el contador de segundos
            myHandler.removeCallbacks(myRunnable);
            myHandler.postDelayed(myRunnable, secondsDelayed * 1000);
        }
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(R.color.colorPrimaryTransDark);
            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions!=null){
                polyline = mapa.addPolyline(lineOptions);
            }else{
                final AlertDialog.Builder mensaje = new AlertDialog.Builder(getContext());
                mensaje.setTitle("Mensaje")
                        .setMessage("\n" +
                                "Ocurrió un problema al consultar la ruta.")
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                            }
                        });
                mensaje.show();
            }
        }
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=true";

        String key = "key="+ getString(R.string.google_maps_key);

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    private String getDistanceUrl(){

        LatLng origin = new LatLng(locUbicacion.getLatitud(), locUbicacion.getLongitud());
        // Origin of route
        String str_origin = "origins="+origin.latitude+","+origin.longitude;

        String destinos = "";
        int contador = 0;
        for(PuntoInteres puntodestino : puntoInteresList){
            contador++;
            destinos = destinos + puntodestino.getLatitud() + "," + puntodestino.getLongitud();
            if(contador<puntoInteresList.size()){
                destinos = destinos + "|";
            }
        }
        // Destination of route
        String str_dest = "destinations="+destinos;

        // Sensor enabled
        String sensor = "sensor=true";

        String key = "key="+ getString(R.string.google_maps_key);

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/distancematrix/"+output+"?"+parameters;

        return url;
    }

    private void getDistances(){
        /*LatLng origin = new LatLng(locUbicacion.getLatitud(), locUbicacion.getLongitud());
        String str_origin = origin.latitude+","+origin.longitude;
        String destinos = "";
        int contador = 0;
        for(PuntoInteres puntodestino : puntoInteresList){
            contador++;
            destinos = destinos + puntodestino.getLatitud() + "," + puntodestino.getLongitud();
            if(contador<puntoInteresList.size()){
                destinos = destinos + "|";
            }
        }
        APIRestful.getResultDistance(str_origin, destinos);*/
    }
}
