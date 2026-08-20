package com.vocsy.epub_viewer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.FlutterPlugin;

import androidx.annotation.NonNull;

/**
 * EpubReaderPlugin
 */
public class EpubViewerPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    private Reader reader;
    private ReaderConfig config;
    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink sink;

    // Made these non-static for better lifecycle management
    private Activity activity;
    private Context context;
    private BinaryMessenger messenger;

    private static final String channelName = "vocsy_epub_viewer";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        messenger = binding.getBinaryMessenger();
        context = binding.getApplicationContext();
        if (context instanceof Application) {
            FolioZipCloser.register((Application) context);
        } else if (context != null) {
            FolioZipCloser.register((Application) context.getApplicationContext());
        }

        // Set up the event channel
        setupEventChannel();

        // Set up the method channel
        channel = new MethodChannel(binding.getBinaryMessenger(), channelName);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
            eventChannel = null;
        }
        messenger = null;
        context = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // Keep activity reference during config changes
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        activity = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    private void setupEventChannel() {
        eventChannel = new EventChannel(messenger, "page");
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                sink = eventSink;
                if (sink == null) {
                    Log.i("empty", "Sink is empty");
                }
            }

            @Override
            public void onCancel(Object o) {
                sink = null;
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "setConfig":
                handleSetConfig(call, result);
                break;
            case "open":
                handleOpen(call, result);
                break;
            case "close":
                handleClose(call, result);
                break;
            case "setChannel":
                handleSetChannel(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void handleSetConfig(MethodCall call, Result result) {
        try {
            Map<String, Object> arguments = (Map<String, Object>) call.arguments;
            String identifier = arguments.get("identifier").toString();
            String themeColor = arguments.get("themeColor").toString();
            String scrollDirection = arguments.get("scrollDirection").toString();
            Boolean nightMode = Boolean.parseBoolean(arguments.get("nightMode").toString());
            Boolean allowSharing = Boolean.parseBoolean(arguments.get("allowSharing").toString());
            Boolean enableTts = Boolean.parseBoolean(arguments.get("enableTts").toString());

            config = new ReaderConfig(context, identifier, themeColor,
                    scrollDirection, allowSharing, enableTts, nightMode);
            if (config.config != null) {
                com.folioreader.util.AppUtil.Companion.saveConfig(context, config.config);
            }

            result.success(null);
        } catch (Exception e) {
            result.error("CONFIG_ERROR", "Failed to set config: " + e.getMessage(), null);
        }
    }

    private void handleOpen(MethodCall call, Result result) {
        try {
            Map<String, Object> arguments = (Map<String, Object>) call.arguments;
            String bookPath = arguments.get("bookPath").toString();
            String lastLocation = arguments.get("lastLocation").toString();

            Log.i("opening", "In open function");

            if (sink == null) {
                Log.i("sink status", "sink is empty");
            }

            Context openContext = activity != null ? activity : context;
            if (config == null) {
                config = new ReaderConfig(openContext, "book", "#FFE9C79A",
                        "alldirections", false, false, false);
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            reader = new Reader(openContext, messenger, config, sink);
            reader.open(bookPath, lastLocation);

            result.success(null);
        } catch (Exception e) {
            result.error("OPEN_ERROR", "Failed to open book: " + e.getMessage(), null);
        }
    }

    private void handleClose(MethodCall call, Result result) {
        try {
            if (reader != null) {
                reader.close();
            }
            result.success(null);
        } catch (Exception e) {
            result.error("CLOSE_ERROR", "Failed to close book: " + e.getMessage(), null);
        }
    }

    private void handleSetChannel(MethodCall call, Result result) {
        try {
            setupEventChannel();
            result.success(null);
        } catch (Exception e) {
            result.error("CHANNEL_ERROR", "Failed to set channel: " + e.getMessage(), null);
        }
    }

}
