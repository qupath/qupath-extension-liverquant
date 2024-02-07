# QuPath Liverquant extension

A QuPath extension to run the [Liverquant](https://github.com/mfarzi/liverquant) algorithm.

The extension is intended for QuPath v0.5 and later. It is not compatible with earlier QuPath versions.

## Installing

To install the Liverquant extension, download the latest `qupath-extension-liverquant-[version].jar` file from [releases](https://github.com/rylern/qupath-extension-liverquant/releases) and drag it onto the main QuPath window.

If you haven't installed any extensions before, you'll be prompted to select a QuPath user directory. The extension will then be copied to a location inside that directory.

You might then need to restart QuPath (but not your computer).

## Scripting

Script examples are located in the `sample-scripts` folder. They show how the extension can be used from scripts (with or without the graphical user interface).

## Building

You can build the extension using OpenJDK 17 or later with

```
gradlew clean build
```

The output will be under `build/libs`. You can drag the jar file on top of QuPath to install the extension.