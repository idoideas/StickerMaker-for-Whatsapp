/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.idoideas.stickermaker.WhatsAppBasedCode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.soloader.SoLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.idoideas.stickermaker.BuildConfig;
import com.idoideas.stickermaker.DataArchiver;
import com.idoideas.stickermaker.NewUserIntroActivity;
import com.idoideas.stickermaker.R;
import com.idoideas.stickermaker.StickerBook;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import co.mobiwise.materialintro.shape.Focus;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.shape.ShapeType;
import co.mobiwise.materialintro.view.MaterialIntroView;

import static com.idoideas.stickermaker.NewUserIntroActivity.verifyStoragePermissions;


public class StickerPackListActivity extends BaseActivity {
    public static final String EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_list";
    private static final int STICKER_PREVIEW_DISPLAY_LIMIT = 5;
    private static final String TAG = "StickerPackList";
    private LinearLayoutManager packLayoutManager;
    private static RecyclerView packRecyclerView;
    private static StickerPackListAdapter allStickerPacksListAdapter;
    WhiteListCheckAsyncTask whiteListCheckAsyncTask;
    ArrayList<StickerPack> stickerPackList;
    public static Context context;
    public static String newName, newCreator;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private BillingClient mBillingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_list);

        MobileAds.initialize(this, getString(R.string.admob_ad_id));
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest
                .Builder()
                .addTestDevice(getString(R.string.test_device))
                .build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.admob_fullscreen_adding_pack_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build());

        StickerBook.init(this);

        Fresco.initialize(this);

        context = getApplicationContext();

        SoLoader.init(this, /* native exopackage */ false);

        mBillingClient = BillingClient.newBuilder(this).setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                }
            }
            @Override
            public void onBillingServiceDisconnected() {

            }
        });

        packRecyclerView = findViewById(R.id.sticker_pack_list);
        stickerPackList = StickerBook.getAllStickerPacks();//getIntent().getParcelableArrayListExtra( EXTRA_STICKER_PACK_LIST_DATA);
        showStickerPackList(stickerPackList);

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            Bundle extras = getIntent().getExtras();
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                if(uri!=null){
                    DataArchiver.importZipFileToStickerPack(uri, StickerPackListActivity.this);
                }
            }
        }

        if(toShowIntro()){
            startActivityForResult(new Intent(this, NewUserIntroActivity.class), 1114);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        String action = getIntent().getAction();
        if(action == null) {
            Log.v("Example", "Force restart");
            Intent intent = new Intent(this, StickerPackListActivity.class);
            intent.setAction("Already created");
            startActivity(intent);
            finish();
        }

        whiteListCheckAsyncTask = new WhiteListCheckAsyncTask(this);
        //noinspection unchecked
        whiteListCheckAsyncTask.execute(stickerPackList);
    }


    @Override
    protected void onPause() {
        super.onPause();
        DataArchiver.writeStickerBookJSON(StickerBook.getAllStickerPacks(), this);
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask.isCancelled()) {
            whiteListCheckAsyncTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        DataArchiver.writeStickerBookJSON(StickerBook.getAllStickerPacks(), this);
        super.onDestroy();
    }


    public void showStickerPackList(List<StickerPack> stickerPackList) {
        allStickerPacksListAdapter = new StickerPackListAdapter(stickerPackList, onAddButtonClickedListener);
        packRecyclerView.setAdapter(allStickerPacksListAdapter);
        packLayoutManager = new LinearLayoutManager(this);
        packLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                packRecyclerView.getContext(),
                packLayoutManager.getOrientation()
        );
        packRecyclerView.addItemDecoration(dividerItemDecoration);
        packRecyclerView.setLayoutManager(packLayoutManager);
        packRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::recalculateColumnCount);
    }



    private StickerPackListAdapter.OnAddButtonClickedListener onAddButtonClickedListener = new StickerPackListAdapter.OnAddButtonClickedListener() {
        @Override
        public void onAddButtonClicked(StickerPack pack) {
            if(pack.getStickers().size()>=3) {
                Intent intent = new Intent();
                intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
                intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, pack.identifier);
                intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY);
                intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_NAME, pack.name);
                try {
                    StickerPackListActivity.this.startActivityForResult(intent, StickerPackDetailsActivity.ADD_PACK);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(StickerPackListActivity.this, R.string.error_adding_sticker_pack, Toast.LENGTH_LONG).show();
                }
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(StickerPackListActivity.this)
                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create();
                alertDialog.setTitle(context.getString(R.string.invalid_action));
                alertDialog.setMessage(getString(R.string.in_order_to_be_applied));
                alertDialog.show();
            }
        }
    };

    private void recalculateColumnCount() {
        final int previewSize = getResources().getDimensionPixelSize(R.dimen.sticker_pack_list_item_preview_image_size);
        int firstVisibleItemPosition = packLayoutManager.findFirstVisibleItemPosition();
        StickerPackListItemViewHolder viewHolder = (StickerPackListItemViewHolder) packRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition);
        if (viewHolder != null) {
            final int max = Math.max(viewHolder.imageRowView.getMeasuredWidth() / previewSize, 1);
            int numColumns = Math.min(STICKER_PREVIEW_DISPLAY_LIMIT, max);
            allStickerPacksListAdapter.setMaxNumberOfStickersInARow(numColumns);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StickerPackDetailsActivity.ADD_PACK) {
            if (resultCode == Activity.RESULT_CANCELED && data != null) {
                final String validationError = data.getStringExtra("validation_error");
                if (validationError != null) {
                    if (BuildConfig.DEBUG) {
                        //validation error should be shown to developer only, not users.
                        MessageDialogFragment.newInstance(R.string.title_validation_error, validationError).show(getSupportFragmentManager(), "validation error");
                    }
                    Log.e(TAG, "Validation failed:" + validationError);
                }
            } else {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                    mInterstitialAd = new InterstitialAd(this);
                    mInterstitialAd.setAdUnitId(getString(R.string.admob_fullscreen_adding_pack_unit_id));
                    mInterstitialAd.loadAd(new AdRequest.Builder().addTestDevice(getString(R.string.test_device)).build());
                }
            }
        } else if (data!=null && requestCode==2319){
            Uri uri = data.getData();
            getContentResolver().takePersistableUriPermission(Objects.requireNonNull(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            createNewStickerPackAndOpenIt(newName, newCreator, uri);
        } else if(requestCode == 1114){
            makeIntroNotRunAgain();

            new MaterialIntroView.Builder(this)
                    .enableIcon(false)
                    .setFocusGravity(FocusGravity.CENTER)
                    .setFocusType(Focus.MINIMUM)
                    .setDelayMillis(500)
                    .enableFadeAnimation(true)
                    .performClick(true)
                    .setInfoText(getString(R.string.to_add_new_sticker))
                    .setShape(ShapeType.CIRCLE)
                    .setTarget(findViewById(R.id.action_add))
                    .setUsageId("intro_card") //THIS SHOULD BE UNIQUE ID
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            addNewStickerPackInInterface();
            return true;
        } else if(item.getItemId() == R.id.action_info){
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.about_layout, null);

            dialogView.findViewById(R.id.redditlogo).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.reddit.com/u/idoideas"));
                    startActivity(browserIntent);
                }
            });

            dialogView.findViewById(R.id.twitterlogo).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.twitter.com/idoideas"));
                    startActivity(browserIntent);
                }
            });

            dialogView.findViewById(R.id.githublogo).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.github.com/idoideas"));
                    startActivity(browserIntent);
                }
            });

            dialogBuilder.setView(dialogView);
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        } else if (item.getItemId() == R.id.action_donate){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Thank you for donation!");
            builder.setMessage("We appreciate your support in great apps and open-source projects.\n\nHow much would you like to donate?");
            builder.setPositiveButton("A Sandwich - 5$",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                            startInAppPurchase("5_dollar_donation");
                        }
                    });

            builder.setNeutralButton("A Piece of Gum - 1$",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                            startInAppPurchase("1_dollar_donation");
                        }
                    });

            builder.setNegativeButton("A Coffee - 3$",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                            startInAppPurchase("3_dollar_donation");
                        }
                    });
            builder.create().show();
        }
        return super.onOptionsItemSelected(item);
    }


    static class WhiteListCheckAsyncTask extends AsyncTask<List<StickerPack>, Void, List<StickerPack>> {
        private final WeakReference<StickerPackListActivity> stickerPackListActivityWeakReference;

        WhiteListCheckAsyncTask(StickerPackListActivity stickerPackListActivity) {
            this.stickerPackListActivityWeakReference = new WeakReference<>(stickerPackListActivity);
        }

        @SafeVarargs
        @Override
        protected final List<StickerPack> doInBackground(List<StickerPack>... lists) {
            List<StickerPack> stickerPackList = lists[0];
            final StickerPackListActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity == null) {
                return stickerPackList;
            }
            for (StickerPack stickerPack : stickerPackList) {
                stickerPack.setIsWhitelisted(WhitelistCheck.isWhitelisted(stickerPackListActivity, stickerPack.identifier));
            }
            return stickerPackList;
        }

        @Override
        protected void onPostExecute(List<StickerPack> stickerPackList) {
            final StickerPackListActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity != null) {
                stickerPackListActivity.allStickerPacksListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setPositiveButton(getString(R.string.lets_go), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    verifyStoragePermissions(StickerPackListActivity.this);
                                }
                            })
                            .create();
                    alertDialog.setTitle("Notice!");
                    alertDialog.setMessage("We've recognized you denied the storage access permission for this app."
                            + "\n\nIn order for this app to work, storage access is required.");
                    alertDialog.show();
                }
                break;
        }
    }

    private void addNewStickerPackInInterface(){

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.create_new_pack));
        dialog.setMessage(getString(R.string.name_creator));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText nameBox = new EditText(this);
        nameBox.setLines(1);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLayoutParams.setMargins(50, 0, 50, 10);
        nameBox.setLayoutParams(buttonLayoutParams);
        nameBox.setHint(getString(R.string.pack_name));
        nameBox.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        nameBox.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

        layout.addView(nameBox);

        final EditText creatorBox = new EditText(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            creatorBox.setAutofillHints(getString(R.string.name));
        }
        creatorBox.setLines(1);
        creatorBox.setLayoutParams(buttonLayoutParams);
        creatorBox.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        creatorBox.setHint(getString(R.string.creator));
        layout.addView(creatorBox);

        dialog.setView(layout);

        dialog.setPositiveButton("OK", null);

        dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        final AlertDialog ad = dialog.create();

        ad.show();

        Button b = ad.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(nameBox.getText())){
                    nameBox.setError(getString(R.string.name_required));
                }

                if(TextUtils.isEmpty(creatorBox.getText())){
                    creatorBox.setError(getString(R.string.creator_required));
                }

                if(!TextUtils.isEmpty(nameBox.getText()) && !TextUtils.isEmpty(creatorBox.getText())) {
                    ad.dismiss();
                    createDialogForPickingIconImage(nameBox, creatorBox);
                }
            }
        });

        creatorBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    b.performClick();
                }
                return false;
            }
        });
    }

    private void createDialogForPickingIconImage(EditText nameBox, EditText creatorBox){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.pick_icon_image));
        builder.setMessage(getString(R.string.pick_new_icon))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.lets_go), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        openFileTray(nameBox.getText().toString(), creatorBox.getText().toString());
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void createNewStickerPackAndOpenIt(String name, String creator, Uri trayImage){
        String newId = UUID.randomUUID().toString();
        StickerPack sp = new StickerPack(
                newId,
                name,
                creator,
                trayImage,
                "",
                "",
                "",
                "",
                this);
        StickerBook.addStickerPackExisting(sp);

        Intent intent = new Intent(this, StickerPackDetailsActivity.class);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, true);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_DATA, newId);
        intent.putExtra("isNewlyCreated", true);
        this.startActivity(intent);
    }

    private void openFileTray(String name, String creator) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        newName = name;
        newCreator = creator;
        startActivityForResult(i, 2319);
    }

    private void makeIntroNotRunAgain(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean("isAlreadyShown", false);
        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("isAlreadyShown", false);
            edit.commit();
        }
    }

    private boolean toShowIntro(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getBoolean("isAlreadyShown", true);
    }

    private void startInAppPurchase(String sku){
        mBillingClient.consumeAsync("", new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(int responseCode, String purchaseToken) {

            }
        });
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku(sku)
                .setType(BillingClient.SkuType.INAPP)
                .build();
        mBillingClient.launchBillingFlow(StickerPackListActivity.this, flowParams);
        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@BillingClient.BillingResponse int responseCode, String outToken) {
                if (responseCode == BillingClient.BillingResponse.OK) {
                    Toast.makeText(getApplicationContext(), getString(R.string.thack_for_donation), Toast.LENGTH_LONG).show();
                }
            }};
        if (purchasesResult!=null){
            if(purchasesResult.getPurchasesList()!=null){
                if(purchasesResult.getPurchasesList().size()>0){
                    for (int i = 0; i<purchasesResult.getPurchasesList().size(); i++){
                        mBillingClient.consumeAsync(purchasesResult.getPurchasesList().get(i).getPurchaseToken(), listener);
                    }
                }
            }
        }
    }

}
