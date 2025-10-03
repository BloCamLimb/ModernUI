/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.RoundedImageDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.Log;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.AbsListView;
import icyllis.modernui.widget.BaseAdapter;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ListView;
import icyllis.modernui.widget.StaggeredGridView;
import icyllis.modernui.widget.TextView;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

public class TestStaggeredGridView extends Fragment {

    static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(
            SimpleDateFormat.LONG, SimpleDateFormat.LONG);

    static class ItemInfo {

        String timestamp;
        String initiator;
        String initiatorName;

        int messageType;
        String message;

        String imageThumb;

        Image avatarImage;
        Image imageThumbImage;

        CompletableFuture<Bitmap> avatarImageReq;
        CompletableFuture<Bitmap> imageThumbImageReq;

        ItemInfo(JsonObject o) {
            timestamp = DATE_FORMAT.format(new Date(o.get("timestamp").getAsLong()));
            initiator = o.get("initiator").getAsString();
            initiatorName = o.get("initiatorName").getAsString();
            messageType = o.get("messageType").getAsInt();
            message = o.get("message").getAsString();
            imageThumb = o.get("image").getAsJsonObject().get("thumb").getAsString();
            if (initiatorName.isEmpty()) {
                initiatorName = "Anonymous";
            }
        }

        void requestImages(ImageLoader loader, List<CompletableFuture<?>> out) {
            {
                String initiator = this.initiator;
                if (initiator.isEmpty() || initiator.equals("00000000-0000-0000-0000-000000000000")) {
                    initiator = "X-Alex";
                }
                avatarImageReq = loader.request("https://vzge.me/face/256/" + initiator + ".png");
                if (avatarImageReq != null) {
                    out.add(avatarImageReq);
                }
            }
            if (imageThumb != null && !imageThumb.isEmpty()) {
                imageThumbImageReq = loader.request(imageThumb);
                if (imageThumbImageReq != null) {
                    out.add(imageThumbImageReq);
                }
            }
        }

        void uploadToTexture() {
            if (avatarImageReq != null) {
                try (Bitmap bm = avatarImageReq.join()) {
                    avatarImage = Image.createTextureFromBitmap(bm);
                } catch (Exception e) {
                    Log.LOGGER.info("Failed to create texture", e);
                }
                avatarImageReq = null;
            }
            if (imageThumbImageReq != null) {
                try (Bitmap bm = imageThumbImageReq.join()) {
                    imageThumbImage = Image.createTextureFromBitmap(bm);
                } catch (Exception e) {
                    Log.LOGGER.info("Failed to create texture", e);
                }
                imageThumbImageReq = null;
            }
        }
    }

    static class ImageLoader {

        HttpClient httpClient;

        ImageLoader() {
            httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(1)).build();
        }

        CompletableFuture<Bitmap> request(String url) {
            try {
                var req = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .header("User-Agent", "Java-http-client/17 (ModernUI 3.12.0)")
                        .build();
                return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
                        .thenApply(res -> {
                            ByteBuffer p = null;
                            try (var is = res.body()) {
                                p = Core.readIntoNativeBuffer(is);
                                p.flip();
                                return BitmapFactory.decodeBuffer(p, null);
                            } catch (IOException e) {
                                Log.LOGGER.info("{}\nURI {}\nContent-Type {}\n{}", e.getMessage(),
                                        res.request().uri(),
                                        res.headers().firstValue("Content-Type").orElse(""),
                                        MemoryUtil.memUTF8Safe(p));
                                return null;
                            } finally {
                                MemoryUtil.memFree(p);
                            }
                        });
            } catch (Exception e) {
                Log.LOGGER.info("Failed to request image {}", url);
                return null;
            }
        }
    }

    AbsListView mGridView;
    MyAdapter mAdapter;

    @Override
    public void onCreate(DataSet savedInstanceState) {
        super.onCreate(savedInstanceState);

        CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://mc.zbx1425.cn/teacon-jiachen/subnoteica_data.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(60_000); // 1min
                connection.setReadTimeout(180_000); // 3min
                connection.connect();
                JsonObject res;
                try (var br = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    res = new Gson().fromJson(br, JsonObject.class);
                }
                var value = res.get("value").getAsJsonArray();
                int limit = Math.min(value.size(), 200);
                var infos = new ArrayList<ItemInfo>();
                for (int i = 0; i < limit; i++) {
                    infos.add(new ItemInfo(value.get(i).getAsJsonObject()));
                }
                List<CompletableFuture<?>> futures = new ArrayList<>();
                ImageLoader loader = new ImageLoader();
                for (var info : infos) {
                    info.requestImages(loader, futures);
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .join();
                return infos;
            } catch (IOException e) {
                throw new CompletionException(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).whenCompleteAsync((res, ex) -> {
            if (ex != null) {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                Log.LOGGER.warn("Failed to request data", cause);
            } else {
                for (var info : res) {
                    info.uploadToTexture();
                }
                mAdapter = new MyAdapter(res);
                mGridView.setAdapter(mAdapter);
            }
        }, Core.getUiThreadExecutor());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {

        var gridView = new StaggeredGridView(requireContext());
        gridView.setColumnWidth(gridView.dp(360));
        gridView.setVerticalSpacing(gridView.dp(8));
        gridView.setHorizontalSpacing(gridView.dp(8));
        //gridView.setDividerHeight(gridView.dp(8));
        int dp16 = gridView.dp(16);
        gridView.setPadding(dp16, dp16, dp16, dp16);
        gridView.setClipToPadding(false);
        gridView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

        return mGridView = gridView;
    }

    static class MyAdapter extends BaseAdapter {

        final ArrayList<ItemInfo> mItemInfos;

        MyAdapter(ArrayList<ItemInfo> itemInfos) {
            mItemInfos = itemInfos;
        }

        @Override
        public int getCount() {
            return mItemInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return mItemInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LinearLayout layout;
            if (convertView != null) {
                layout = (LinearLayout) convertView;
            } else {
                layout = createLayout(parent.getContext());
            }

            ItemInfo data = mItemInfos.get(position);

            ImageView imageView = layout.requireViewById(R.id.icon);
            if (data.imageThumbImage != null) {
                var imageDrawable = new RoundedImageDrawable(layout.getContext().getResources(), data.imageThumbImage);
                imageDrawable.setCornerRadius(layout.dp(12));
                imageView.setImageDrawable(imageDrawable);
            } else {
                imageView.setImageDrawable(null);
            }
            imageView = layout.requireViewById(R.id.icon1);
            if (data.avatarImage != null) {
                var imageDrawable = new RoundedImageDrawable(layout.getContext().getResources(), data.avatarImage);
                imageDrawable.setCircular(true);
                imageView.setImageDrawable(imageDrawable);
            } else {
                imageView.setImageDrawable(null);
            }
            TextView textView = layout.requireViewById(R.id.title);
            if (data.message == null || data.message.isEmpty()) {
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                textView.setText(data.message);
            }
            textView = layout.requireViewById(R.id.text1);
            textView.setText(data.initiatorName);
            textView = layout.requireViewById(R.id.text2);
            textView.setText(data.timestamp);
            layout.setContentDescription(data.message);

            return layout;
        }

        private LinearLayout createLayout(Context context) {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            makeElevatedCard(context, layout, new TypedValue());

            int dp12 = layout.dp(12);
            int dp16 = layout.dp(16);

            {
                ImageView imageView = new ImageView(context);
                imageView.setId(R.id.icon);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                layout.addView(imageView, MATCH_PARENT, WRAP_CONTENT);
            }

            {
                TextView textView = new TextView(context);
                textView.setId(R.id.title);
                textView.setMaxLines(2);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setTextStyle(Typeface.BOLD);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.setMargins(dp12, dp16, dp12, 0);
                layout.addView(textView, params);
            }

            {
                LinearLayout row = new LinearLayout(context);

                {
                    ImageView avatarView = new ImageView(context);
                    avatarView.setId(R.id.icon1);
                    avatarView.setScaleType(ImageView.ScaleType.FIT_XY);
                    row.addView(avatarView, layout.dp(32), layout.dp(32));
                }

                {
                    LinearLayout col = new LinearLayout(context);
                    col.setOrientation(LinearLayout.VERTICAL);

                    {
                        TextView name = new TextView(context);
                        name.setId(R.id.text1);
                        name.setTextAppearance(R.attr.textAppearanceLabelLarge);
                        col.addView(name, MATCH_PARENT, WRAP_CONTENT);
                    }
                    {
                        TextView date = new TextView(context);
                        date.setId(R.id.text2);
                        date.setTextAppearance(R.attr.textAppearanceLabelMedium);
                        col.addView(date, MATCH_PARENT, WRAP_CONTENT);
                    }

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1F);
                    params.setMarginStart(dp12);
                    row.addView(col, params);
                }

                row.setGravity(Gravity.CENTER);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.setMargins(dp12, dp16, dp12, dp12);
                layout.addView(row, params);
            }

            return layout;
        }

        public static void makeElevatedCard(@Nonnull Context context, @Nonnull View layout,
                                            @Nonnull TypedValue value) {
            final int dp12 = layout.dp(12);
            ShapeDrawable bg = new ShapeDrawable();
            bg.setCornerRadius(dp12);
            context.getTheme().resolveAttribute(R.ns, R.attr.colorSurfaceContainerLow, value, true);
            bg.setColor(value.data);
            layout.setBackground(bg);
            layout.setElevation(layout.dp(1));
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}
