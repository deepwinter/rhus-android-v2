package org.calflora.observer;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import net.winterroot.android.wildflowers.R;
import net.winterroot.rhus.util.DWUtilities;

import org.calflora.map.OfflineMapTileProvider;
import org.odk.collect.android.views.ODKView;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;


public class ObservationMainFragment extends ObservationODKFragment {

	private View layout = null;
	private GoogleMap map;
	private MapFragment mapFragment;
	@SuppressWarnings("unused")
	private String TAG = "ObservationSummaryFragment";
    private Uri mPhotoUri;
	
    static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;

	public ObservationMainFragment(ODKView odkv) {
		super(odkv);
		this.odkv = odkv;
	}
	


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		if(layout == null) {
			RelativeLayout partialLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_observation_summary, container, false);
			
			LinearLayout formLayout = (LinearLayout) odkv.getChildAt(0);
			formLayout.addView(partialLayout, 0);
			RelativeLayout fullLayout = new RelativeLayout(getActivity());
			fullLayout.addView(odkv);
			layout = fullLayout;
		}
		return layout;
	}

	Boolean lockUI = false;
	Object lock = new Object();


	@Override
	public void onStart() {
		super.onStart();

		Button captureImageButton = (Button) getView().findViewById(R.id.plant_photo_image_button);
		captureImageButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v){

				synchronized(lock) {

					if(lockUI){
						return;
					}

					if(!lockUI){
						lockUI = true;
					}

				}

				//Intent intent = new Intent("org.calflora.observer.action.CAPTUREPHOTO");
				//startActivityForResult(intent, CAPTURE_PHOTO);  

				mPhotoUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
				startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

			}
		});


	}



	@Override
	public void onStop() {
		super.onStop();

		synchronized(lock) {
			lockUI = false;
		}
	}



	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// TODO Is this the right place for this in the lifecycle?
		// Put this here to ensure we don't get a null map
		mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
		map = mapFragment.getMap();
		if(map != null) {
			//Avoids a crash
			map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			map.setMyLocationEnabled(false);

			// Custom offline layer.
			//map.addTileOverlay(new TileOverlayOptions().tileProvider(new OfflineMapTileProvider(getResources().getAssets(), "yosemiteoffice")));

			LatLng latLng = new LatLng( Observer.getInstance().getLastLocation().getLatitude(), Observer.getInstance().getLastLocation().getLongitude());
			map.moveCamera( CameraUpdateFactory.newLatLngZoom(latLng, 13) );

			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(latLng);
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
			map.addMarker(markerOptions);

		} else {
			if(ConnectionResult.SUCCESS != GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity() ) ){
				Observer.toast("Google Maps Not Available", getActivity());
			} else {
				Observer.toast("Google Maps Load Error", getActivity());
			}
		}
	}

    private int exifOrientationToDegrees(int orientation) {
        switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            return 90;
        case ExifInterface.ORIENTATION_ROTATE_180:
            return 180;
        case ExifInterface.ORIENTATION_ROTATE_270:
            return -90;
        default:
            return 0;
        }

    }

    private void updateImageOrientation(Uri uri) {
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.Media.DATA
        };
        Cursor c = getActivity().getContentResolver().query(uri, projection, null, null, null);
        c.moveToFirst();
        String imgFilePath = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        ContentValues values = new ContentValues();
        try {
            ExifInterface exif = new ExifInterface(imgFilePath);
            int degrees = exifOrientationToDegrees(
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
                            ExifInterface.ORIENTATION_NORMAL));
            values.put(MediaStore.Images.ImageColumns.ORIENTATION, degrees);
            getActivity().getContentResolver().update(uri, values, null, null);
        } catch (IOException e) {
            Log.e(TAG, "couldn't find " + imgFilePath);
        }
    }
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{

		if(!(resultCode == Activity.RESULT_OK)){
			Observer.toast("Error capturing image: result code is not OK", getActivity());
			return;
		}


		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode ==  Activity.RESULT_OK) {
            	
            	
                updateImageOrientation(mPhotoUri);
                
                String[] projection = {
                        MediaStore.MediaColumns._ID,
                        MediaStore.Images.ImageColumns.ORIENTATION,
                        MediaStore.Images.Media.DATA
                };
                Cursor c = getActivity().getContentResolver().query(mPhotoUri, projection, null, null, null);
                c.moveToFirst();
                String photoFileName = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                int orientation = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
                
                Bitmap bitmap = BitmapFactory.decodeFile(photoFileName);
                Matrix matrix = new Matrix();
                matrix.setRotate((float) orientation, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
     
				
				// Get the full quality image bytes
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
				byte[] plantImageBytes = stream.toByteArray();
				
				//Now resize a thumbnail
				Bitmap square;
				if (bitmap.getWidth() >= bitmap.getHeight()){

					square = Bitmap.createBitmap(
							bitmap, 
							bitmap.getWidth()/2 - bitmap.getHeight()/2,
							0,
							bitmap.getHeight(), 
							bitmap.getHeight()
							);

				} else {

					square = Bitmap.createBitmap(
							bitmap,
							0, 
							bitmap.getHeight()/2 - bitmap.getWidth()/2,
							bitmap.getWidth(),
							bitmap.getWidth() 
							);
				}

				matrix = new Matrix();
				
				// RESIZE THE BIT MAP
				// According to a variety of resources, this function should give us pixels from the dp of the screen
				// From http://stackoverflow.com/questions/4605527/converting-pixels-to-dp-in-android
				float targetHeight = DWUtilities.convertDpToPixel(80, getActivity());
				float targetWidth = DWUtilities.convertDpToPixel(80, getActivity());
				
				// However, the above pixel dimension are still too small to show in my 80dp image view
				// On the Nexus 4, a factor of 4 seems to get us up to the right size
				// No idea why.
				targetHeight *= 4;
				targetWidth *= 4;
									
				matrix.postScale( (float) targetHeight / square.getWidth(), (float) targetWidth / square.getHeight());
				Bitmap resizedBitmap = Bitmap.createBitmap(square, 0, 0, square.getWidth(), square.getHeight(), matrix, false); 
				
				// By the way, the below code also gives a full size, but blurry image
				// Bitmap resizedBitmap = Bitmap.createScaledBitmap(square, (int) targetWidth, (int) targetHeight, false);
				
				// NOTE the density of the image is not necessarily equivalent to the screen density
				
				// Get the bytes for storage
				stream = new ByteArrayOutputStream();
				resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
				byte[] thumbBytes = stream.toByteArray();

				// Add the attachments
				try {
					Observer.currentObservation.setAttachment("thumbnail", thumbBytes, "image/jpeg", getActivity());
					Observer.currentObservation.setAttachment("photo1", plantImageBytes, "image/jpeg", getActivity()); 
				} catch (IOException e) {
					e.printStackTrace();
				}

				ImageView photoThumb = (ImageView) getView().findViewById(R.id.plant_image_thumbnail);
				Button photoButton = (Button) getView().findViewById(R.id.plant_photo_image_button);
				// Update the UI
				if (photoButton != null)
					photoThumb.setImageDrawable(null);
				
				String thumbnailPath = null;
				try {
					thumbnailPath = Observer.currentObservation.getAttachmentPath("thumbnail", getActivity());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(thumbnailPath != null){
					Drawable d = Drawable.createFromPath(thumbnailPath);
					if(d != null){
						photoThumb.setImageDrawable(d);
					}
				}

				photoButton.setText("");

                
                //mHelper.loading("Processing...");
                //Intent intent = new Intent(Intent.ACTION_INSERT, ObservationPhoto.CONTENT_URI, this, ObservationEditor.class);
                //intent.putExtra("photoUri", mPhotoUri);
                //startActivity(intent);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User cancelled the image capture
                getActivity().getContentResolver().delete(mPhotoUri, null, null);
            } else {
                // Image capture failed, advise user
                Toast.makeText(getActivity(), "Blast, something went wrong:\n" + mPhotoUri, Toast.LENGTH_LONG).show();
                Log.e(TAG, "camera bailed, requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + data.getData());
                getActivity().getContentResolver().delete(mPhotoUri, null, null);
            }
            mPhotoUri = null; // don't let this hang around
        }
		
	}
}


