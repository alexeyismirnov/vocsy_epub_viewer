import 'dart:io';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:vocsy_epub_viewer/epub_viewer.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() async {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final platform = MethodChannel('my_channel');
  bool loading = false;
  Dio dio = Dio();
  String filePath = "";
  String LOCATOR_KEY = 'epub_last_locator';

  @override
  void initState() {
    download();
    _setupLocatorListener();

    super.initState();
  }

  /// Setup locator listener to save position
  void _setupLocatorListener() {
    VocsyEpub.locatorStream.listen((locator) {
      print('LOCATOR: $locator');
      _saveLocator(locator);
    });
  }

  /// Save locator to SharedPreferences
  Future<void> _saveLocator(String locator) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final locatorJson = locator;
      await prefs.setString(LOCATOR_KEY, locatorJson);
      print('Locator saved: $locatorJson');
    } catch (e) {
      print('Error saving locator: $e');
    }
  }

  /// Load locator from SharedPreferences
  Future<EpubLocator?> _loadLocator() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final locatorJson = prefs.getString(LOCATOR_KEY);
      if (locatorJson != null) {
        final locatorMap = jsonDecode(locatorJson);
        return EpubLocator.fromJson(locatorMap);
      }
    } catch (e) {
      print('Error loading locator: $e');
    }
    return null;
  }

  /// Clear saved locator
  Future<void> _clearLocator() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(LOCATOR_KEY);
      print('Locator cleared');
    } catch (e) {
      print('Error clearing locator: $e');
    }
  }

  /// ANDROID VERSION
  Future<void> fetchAndroidVersion() async {
    final String? version = await getAndroidVersion();
    if (version != null) {
      String? firstPart;
      if (version.toString().contains(".")) {
        int indexOfFirstDot = version.indexOf(".");
        firstPart = version.substring(0, indexOfFirstDot);
      } else {
        firstPart = version;
      }
      int intValue = int.parse(firstPart);
      if (intValue >= 13) {
        await startDownload();
      } else {
        final PermissionStatus status = await Permission.storage.request();
        if (status == PermissionStatus.granted) {
          await startDownload();
        } else {
          await Permission.storage.request();
        }
      }
      print("ANDROID VERSION: $intValue");
    }
  }

  Future<String?> getAndroidVersion() async {
    try {
      final String version = await platform.invokeMethod('getAndroidVersion');
      return version;
    } on PlatformException catch (e) {
      print("FAILED TO GET ANDROID VERSION: ${e.message}");
      return null;
    }
  }

  download() async {
    if (Platform.isIOS) {
      final PermissionStatus status = await Permission.storage.request();
      if (status == PermissionStatus.granted) {
        final documents = (await getApplicationDocumentsDirectory()).path;
        await Directory("$documents/orthodoxbookshop/").create();

        await startDownload();
      } else {
        await Permission.storage.request();
      }
    } else if (Platform.isAndroid) {
      await fetchAndroidVersion();
    } else {
      PlatformException(code: '500');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Vocsy Plugin E-pub example'),
        ),
        body: Center(
          child: loading
              ? Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    CircularProgressIndicator(),
                    Text('Downloading.... E-pub'),
                  ],
                )
              : Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    ElevatedButton(
                      onPressed: () async {
                        print("=====filePath======$filePath");
                        if (filePath == "") {
                          download();
                        } else {
                          LOCATOR_KEY = "book1";

                          VocsyEpub.setConfig(
                            themeColor: Theme.of(context).primaryColor,
                            identifier: "iosBook",
                            scrollDirection: EpubScrollDirection.ALLDIRECTIONS,
                            allowSharing: true,
                            enableTts: true,
                            nightMode: true,
                          );

                          final savedLocator = await _loadLocator();

                          VocsyEpub.open(filePath, lastLocation: savedLocator);
                        }
                      },
                      child: Text('Open Online E-pub'),
                    ),
                    ElevatedButton(
                      onPressed: () async {
                        LOCATOR_KEY = "book2";

                        VocsyEpub.setConfig(
                          themeColor: Theme.of(context).primaryColor,
                          identifier: "iosBook",
                          scrollDirection: EpubScrollDirection.ALLDIRECTIONS,
                          allowSharing: true,
                          enableTts: true,
                          nightMode: true,
                        );
                        final savedLocator = await _loadLocator();

                        await VocsyEpub.openAsset('assets/4.epub', lastLocation: savedLocator);
                      },
                      child: Text('Open Assets E-pub'),
                    ),
                  ],
                ),
        ),
      ),
    );
  }

  //         "https://filedn.com/lUdNcEH0czFSe8uSnCeo29F/orthodoxbookshop/prayerbookTradit.epub",

  startDownload() async {
    setState(() {
      loading = true;
    });
    Directory? appDocDir = Platform.isAndroid
        ? await getExternalStorageDirectory()
        : await getApplicationDocumentsDirectory();

    final dir = appDocDir!.path + '/orthodoxbookshop';
    await Directory(dir).create();

    String path = "$dir/sample.epub";
    File file = File(path);

    if (!File(path).existsSync()) {
      await file.create();
      await dio.download(
        "https://filedn.com/lUdNcEH0czFSe8uSnCeo29F/orthodoxbookshop/prayerbookTradit.epub",
        path,
        deleteOnError: true,
        onReceiveProgress: (receivedBytes, totalBytes) {
          print('Download --- ${(receivedBytes / totalBytes) * 100}');
          setState(() {
            loading = true;
          });
        },
      ).whenComplete(() {
        setState(() {
          loading = false;
          filePath = path;
        });
      });
    } else {
      setState(() {
        loading = false;
        filePath = path;
      });
    }
  }
}
