package com.vocsy.epub_viewer;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.ui.base.OnSaveHighlight;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;
import com.folioreader.util.AppUtil;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

public class Reader implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener {

    private ReaderConfig readerConfig;
    public FolioReader folioReader;
    private Context context;
    public MethodChannel.Result result;
    private EventChannel eventChannel;
    private EventChannel.EventSink pageEventSink;
    private BinaryMessenger messenger;
    private ReadLocator read_locator;
    private static final String PAGE_CHANNEL = "sage";

    Reader(Context context, BinaryMessenger messenger, ReaderConfig config, EventChannel.EventSink sink) {
        this.context = context;
        readerConfig = config;

        getHighlightsAndSave();
        //setPageHandler(messenger);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);
        pageEventSink = sink;
    }

    public void open(String bookPath, String lastLocation) {
        final String path = bookPath;
        final String location = lastLocation;
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReadLocator locator = null;
                if (location != null && !location.isEmpty()) {
                    try {
                        locator = ReadLocator.fromJson(location);
                    } catch (Exception e) {
                        Log.e("Reader", "Failed to parse ReadLocator: " + e.getMessage());
                    }
                }
                final ReadLocator parsedLocator = locator;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (parsedLocator != null) {
                                folioReader.setReadLocator(parsedLocator);
                            }
                            if (readerConfig != null && readerConfig.config != null) {
                                AppUtil.Companion.saveConfig(context, readerConfig.config);
                                folioReader.setConfig(readerConfig.config, true);
                            }
                            folioReader.openBook(path);
                        } catch (Exception e) {
                            Log.e("Reader", "Error opening book: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    public void close() {
        if (folioReader != null) {
            folioReader.close();
        }
    }

    private void setPageHandler(BinaryMessenger messenger) {
        Log.i("event sink is", "in set page handler:");
        eventChannel = new EventChannel(messenger, PAGE_CHANNEL);

        try {
            eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
                @Override
                public void onListen(Object o, EventChannel.EventSink eventSink) {
                    Log.i("event sink is", "this is event sink:");
                    pageEventSink = eventSink;
                    if (pageEventSink == null) {
                        Log.i("empty", "Sink is empty");
                    }
                }

                @Override
                public void onCancel(Object o) {
                    // Clean up if needed
                }
            });
        } catch (Exception err) {
            Log.e("Reader", "Error setting up page handler: " + err.toString());
        }
    }

    private void getHighlightsAndSave() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HighLight> highlightList = null;
                ObjectMapper objectMapper = new ObjectMapper();

                try {
                    String jsonString = loadAssetTextAsString("highlights/highlights_data.json");
                    if (jsonString != null && !jsonString.isEmpty()) {
                        // FIX 2: Parse as HighLight instead of HighlightData
                        List<HighLight> tempList = objectMapper.readValue(
                                jsonString,
                                new TypeReference<List<HighLight>>() {}
                        );
                        highlightList = new ArrayList<>(tempList);
                    }
                } catch (IOException e) {
                    Log.e("Reader", "Error parsing highlights: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e("Reader", "Unexpected error loading highlights: " + e.getMessage());
                    e.printStackTrace();
                }

                // FIX 3: Only save if highlightList is NOT null and not empty
                if (highlightList != null && !highlightList.isEmpty()) {
                    folioReader.saveReceivedHighLights(highlightList, new OnSaveHighlight() {
                        @Override
                        public void onFinished() {
                            Log.i("Reader", "Successfully saved highlights");
                        }
                    });
                } else {
                    Log.i("Reader", "No highlights to save");
                }
            }
        }).start();
    }

    private String loadAssetTextAsString(String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = context.getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ((str = in.readLine()) != null) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e("Reader", "Error opening asset " + name + ": " + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("Reader", "Error closing asset " + name + ": " + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    public void onFolioReaderClosed() {
        if (read_locator != null) {
            Log.i("readLocator", "-> saveReadLocator -> " + read_locator.toJson());
            if (pageEventSink != null) {
                pageEventSink.success(read_locator.toJson());
            }


        } else {
            Log.w("Reader", "ReadLocator is null when closing");
            if (pageEventSink != null) {
                pageEventSink.success(null);
            }
        }
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
        // Handle highlight actions if needed
        Log.i("Reader", "Highlight action: " + type.toString());
    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        read_locator = readLocator;
        Log.i("Reader", "ReadLocator saved: " + (readLocator != null ? readLocator.toJson() : "null"));
    }
}
