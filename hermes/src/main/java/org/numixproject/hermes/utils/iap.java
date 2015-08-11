package org.numixproject.hermes.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

/**
 * Created by paolo on 11/05/15.
 */
public class iap implements BillingProcessor.IBillingHandler {
    public BillingProcessor bp;

    public BillingProcessor getBilling(Context context, String key){
        bp = new BillingProcessor(context, key, this);
        return bp;
    }

    public boolean isPurchased(){
        return bp.isPurchased(produ)
    }


    // IBillingHandler implementation

    @Override
    public void onBillingInitialized() {
        /*
         * Called when BillingProcessor was initialized and it's ready to purchase
         */
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        /*
         * Called when some error occurred. See Constants class for more details
         */
    }

    @Override
    public void onPurchaseHistoryRestored() {
        /*
         * Called when purchase history was restored and the list of all owned PRODUCT ID's
         * was loaded from Google Play
         */
    }
}
