# Overture 2.1.0 Release Notes — 3 June 2014 

## What's New?

This release is formally releases two major features: the new AST-based Proof Obligation Generator, and the Java code generator.

The new proof obligation generator uses a new internal representation for proof obligations.  The previous POG utilised strings, while the new POG uses ASTs.  This allows for further processing of the POs by other plugins.  From a user's perspective there should be little change --- only a few additional parentheses in the PO expressions.  The new POG is the first step towards supporting automatic discharging of proof obligations, which we hope to add in the near future.  Because of this, the new POG no longer the discharges trivial POs.

The VDM++ to Java code generator is now available as a non-experimental version.  It indicates to the user if a construct cannot be generated by highlighting it in the editor using a marker (similar to the way warnings are shown) and outputs messages about it in the console. Generated code is output in the `generated/java` folder with the VDM++ project in Overture.

The generated Java code depends on a runtime/library to represent VDM collections, VDM types, and so forth.  The source code for the runtime can be found in the Overture Github repository at: <https://github.com/overturetool/overture/tree/development/core/codegen-runtime>.
 

## System Requirements

Overture is based on the Eclipse platform.  To run it, you must have a Java runtime system installed, version 6 or later.


## Download & Installation

Documentation, including tutorials and a language manual, are bundled in the download package, and are also available from the [Overture website](http://www.overturetool.org/).

Overture can be downloaded from GitHub at <https://github.com/overturetool/overture/releases> or from the SourceForge site at <https://sourceforge.net/projects/overture/files/Overture_IDE/>.

The download file is a ZIP archive.  To install the tool, unzip the file in a convenient location.  The main executable is in the top level directory, called `Overture`.

We will be making an exe-based installer available for Windows user at a future point.


### Upgrading:

* ATTENTION: If you are using the ZIP archive to update and if your workspace was located inside the previous installation directory, DO NOT FORGET TO BACKUP the workspace or it will be lost when the old version is deleted.
* If you are upgrading with the ZIP archive: do not unzip the latest version on top of the oldest one.  You will need to delete the previous version before continuing the installation.


### Uninstalling

To uninstall Overture, remove the contents of the directory where you installed it.  There are no other files stored on the system, apart from any workspace files you may have created.

For Windows users, if the Overture installer was used previously, it is possible to uninstall via the uninstall shortcut created by the installer in the start programs or via the Uninstall menu in the Control Panel.


## Reporting Problems and Troubleshooting

Please report bugs, problems, and other issues with the tool at <https://github.com/overturetool/overture/issues>.

If you encounter a problem with the Overture IDE itself, please contact the Overture project and we will try to help.  You can contact us at info@overturetool.org, or use [StackOverflow](http://stackoverflow.com/questions/tagged/vdm%2b%2b) — we monitor for questions using the `vdm`, `vdm++`, or `vdmrt` tags.

If you encounter a problem with a VDM specification, please try to make a small example that illustrates the problem before you contact us.  If you are sure the bug is not already known in the GitHub issues list, you can create a new bug report.


## Frequently Asked Questions

* Who's behind Overture?
> Overture was written by the members of the Overture project, a group of industry and academic researchers interested in the use of VDM.

* How is Overture licensed?
> Overture is open source. It is released under a GPLv3 license.

* What can I do to help?
> If you are interested in helping you can drop a mail to info@overturetool.org.  You are also very welcome to fork our code on GitHub and send us pull requests.

* Where is Overture source code?
> Overture source code is hosted by GitHub, within the [overturetool](https://github.com/overturetool) organisation account.


## Other Resources and Links

* [Overture Community site](http://www.overturetool.org)
* [VDM Tutorials](http://overturetool.org/?q=Documentation)
* [VDM Examples](http://overturetool.org/?q=node/11)
* [VDM Portal](http://www.vdmportal.org)
* [VDM Books](http://www.vdmbook.com)
* [Wikipedia on VDM](http://en.wikipedia.org/wiki/Vienna_Development_Method)
* [Overture Developers Wiki on GitHub](https://github.com/overturetool/overture/wiki/)
* [The Old Overture Wiki for developers](http://wiki.overturetool.org)


## Bug fixes

Please note that the interactive list is at <https://github.com/overturetool/overture/issues?milestone=12&state=closed>

* [#258 Coverage markers are out by one character](https://api.github.com/repos/overturetool/overture/issues/258)
* [#254 Standard Eclipse user interface features "missing" from 2.0.0](https://api.github.com/repos/overturetool/overture/issues/254)
* [#246 The bus declarations given in the vdm real time log produced by the interpreter are incorrect](https://api.github.com/repos/overturetool/overture/issues/246)
* [#207 Editor gives errors while editing a VDM-SL model (with errors)](https://api.github.com/repos/overturetool/overture/issues/207)
* [#198 Inconsistent editor tabstops in Linux/Windows](https://api.github.com/repos/overturetool/overture/issues/198)
* [#195 Warnings with IDE at startup with a large existing workspace](https://api.github.com/repos/overturetool/overture/issues/195)
* [#193 Projects in working sets don't get error marker updates](https://api.github.com/repos/overturetool/overture/issues/193)
* [#188 Syntax error highlightling sometimes goes to first line](https://api.github.com/repos/overturetool/overture/issues/188)
* [#184 VDM++ Editor not correctly showing error locations](https://api.github.com/repos/overturetool/overture/issues/184)
* [#156 External editor doesn't give hover text for error/warnings](https://api.github.com/repos/overturetool/overture/issues/156)
* [#154 Saving](https://api.github.com/repos/overturetool/overture/issues/154)
* [#144 Latex coverage table wrong for overloaded names](https://api.github.com/repos/overturetool/overture/issues/144)
* [#143 Some VDMUnit error strings cause parser errors](https://api.github.com/repos/overturetool/overture/issues/143)
* [#133 Extended explicit functions missing from launchers](https://api.github.com/repos/overturetool/overture/issues/133)
* [#118 Generating coverage with .doc files is broken](https://api.github.com/repos/overturetool/overture/issues/118)
* [#108 Tab expansion in Word coverage output is wrong](https://api.github.com/repos/overturetool/overture/issues/108)
* [#96 Process console not selected when starting a debug](https://api.github.com/repos/overturetool/overture/issues/96)
* [#63 Coverage display japanese chars](https://api.github.com/repos/overturetool/overture/issues/63)
* [#34 UTF-VDM RT Realtime Log can't display UTF-8 Chars](https://api.github.com/repos/overturetool/overture/issues/34)