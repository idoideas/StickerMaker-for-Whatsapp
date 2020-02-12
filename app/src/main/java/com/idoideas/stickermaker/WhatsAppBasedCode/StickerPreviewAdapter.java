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
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.idoideas.stickermaker.R;

public class StickerPreviewAdapter extends RecyclerView.Adapter<StickerPreviewViewHolder> {

    @NonNull
    private StickerPack stickerPack;

    private final int cellSize;
    private int cellLimit;
    private int cellPadding;
    private final int errorResource;

    private final LayoutInflater layoutInflater;

    StickerPreviewAdapter(
            @NonNull final LayoutInflater layoutInflater,
            final int errorResource,
            final int cellSize,
            final int cellPadding,
            @NonNull final StickerPack stickerPack) {
        this.cellSize = cellSize;
        this.cellPadding = cellPadding;
        this.cellLimit = 0;
        this.layoutInflater = layoutInflater;
        this.errorResource = errorResource;
        this.stickerPack = stickerPack;
    }

    @NonNull
    @Override
    public StickerPreviewViewHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int i) {
        View itemView = layoutInflater.inflate(R.layout.sticker_image, viewGroup, false);
        StickerPreviewViewHolder vh = new StickerPreviewViewHolder(itemView);

        ViewGroup.LayoutParams layoutParams = vh.stickerPreviewView.getLayoutParams();
        layoutParams.height = cellSize;
        layoutParams.width = cellSize;
        vh.stickerPreviewView.setLayoutParams(layoutParams);
        vh.stickerPreviewView.setPadding(cellPadding, cellPadding, cellPadding, cellPadding);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final StickerPreviewViewHolder stickerPreviewViewHolder, final int i) {
        Sticker thisSticker = stickerPack.getSticker(i);
        Context thisContext = stickerPreviewViewHolder.stickerPreviewView.getContext();
        stickerPreviewViewHolder.stickerPreviewView.setImageResource(errorResource);
        stickerPreviewViewHolder.stickerPreviewView.setImageURI(thisSticker.getUri());
        stickerPreviewViewHolder.stickerPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView image = new ImageView(thisContext);
                image.setImageURI(thisSticker.getUri());
                AlertDialog alertDialog = new AlertDialog.Builder(thisContext)
                        .setNegativeButton(thisContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton(thisContext.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(stickerPack.getStickers().size()>3 || !WhitelistCheck.isWhitelisted(thisContext, stickerPack.getIdentifier())){
                                    dialogInterface.dismiss();
                                    stickerPack.deleteSticker(thisSticker);
                                    Activity thisActivity = ((Activity)thisContext);
                                    thisActivity.finish();
                                    thisActivity.startActivity(thisActivity.getIntent());
                                    Toast.makeText(thisContext, thisActivity.getString(R.string.sticker_pack_deleted), Toast.LENGTH_SHORT).show();
                                } else {
                                    AlertDialog alertDialog = new AlertDialog.Builder(thisContext)
                                            .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.dismiss();
                                                }
                                            }).create();
                                    alertDialog.setTitle(thisContext.getString(R.string.invalid_action));
                                    alertDialog.setMessage(thisContext.getString(R.string.a_sticker_pack_that) +
                                            thisContext.getString(R.string.in_order_to_remove_additional));
                                    alertDialog.show();
                                }
                            }
                        })
                        .setView(image)
                        .create();
                alertDialog.setTitle(thisContext.getString(R.string.do_you_want_to_delete_this));
                alertDialog.setMessage(thisContext.getString(R.string.deleting_this_sticker_will_also_remove));
                alertDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        int numberOfPreviewImagesInPack;
        numberOfPreviewImagesInPack = stickerPack.getStickers().size();
        if (cellLimit > 0) {
            return Math.min(numberOfPreviewImagesInPack, cellLimit);
        }
        return numberOfPreviewImagesInPack;
    }
}
