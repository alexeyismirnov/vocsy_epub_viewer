# Build instructions

This project depends on [FolioReaderKit](https://github.com/alexeyismirnov/FolioReaderKit). You need to clone
this project and build XCFramework using ```build_xc.sh```. The commands of this script are explained 
[here](https://stackoverflow.com/questions/68359078/build-xcframework-with-pod-dependency).

Then copy ```./build/FolioReaderKit.xcframework/``` to the ```ios``` folder of this plugin. Try to build 
example app, but it will fail, because the XCframework is not in the search path. We'll need to open example project in Xcode
and follow [this excellent article](https://stackoverflow.com/a/70210039/995049). However, there is no need to ```Embed & Sign``` the XCFramework, it is already included in the final binary as part of
```vocsy_epub_viewer```.
In addition, for this project (which you can find in the target list of the ```Pods``` workspace) you need to change ```Build Active Architecture Only -> YES```.

These steps should be performed for every app that uses ```vocsy_epub_viewer```. 

Finally, when building for release, you'll need to [add a new build phase](https://stackoverflow.com/a/78013024/995049) due to a bug in XCode 15.

