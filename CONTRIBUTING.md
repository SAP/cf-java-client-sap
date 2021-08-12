# Contributing to cf-java-client-sap

You want to contribute to to cf-java-client-sap? Welcome! Please read this document to understand what you can do:
 * [Help Others](#help-others)
 * [Analyze Issues](#analyze-issues)
 * [Report an Issue](#report-an-issue)
 * [Contribute Code](#contribute-code)

## Help Others

You can help cf-java-client-sap by helping others who use it and need support.

## Analyze Issues

Analyzing issue reports can be a lot of effort. Any help is welcome!
Go to [the Github issue tracker](https://github.com/SAP/cf-java-client-sap/issues?state=open) and find an open issue which needs additional work or a bugfix.

Additional work may be further information, or gist, or it might be a hint that helps understanding the issue. Maybe you can even find and [contribute](#contribute-code) a bugfix?


## Report an Issue

If you find a bug - behavior of cf-java-client-sap code contradicting its specification - you are welcome to report it.
We can only handle well-reported, actual bugs, so please follow the guidelines below for support questions or when in doubt whether the issue is an actual bug.


### Quick Checklist for Bug Reports

Issue report checklist:
 * Real, current bug
 * No duplicate
 * Reproducible
 * Good summary
 * Well-documented
 * Minimal example


### Requirements for a bug report

These eight requirements are the mandatory base of a good bug report:
1. **Only real bugs**: please do your best to make sure to only report real bugs in cf-client-java-sap! Do not report:
   * issues caused by library code or any code outside the project.
   * issues caused by the usage of non-public methods. Only the public methods listed in the API documentation may be used.
   * something that behaves just different from what you expected. A bug is when something behaves different than specified. When in doubt, ask in a forum.
   * something you do not get to work properly. Use a support forum like stackoverflow to request help.
   * feature requests. Well, this is arguable: critical or easy-to-do enhancement suggestions are welcome, but we do not want to use the issue tracker as wishlist.
2. No duplicate: you have searched the issue tracker to make sure the bug has not yet been reported
3. Good summary: the summary should be specific to the issue
4. Current bug: the bug can be reproduced in the most current version (state the tested version!)
5. Reproducible bug: there are clear steps to reproduce given. This includes:
   * any required user/password information (do not reveal any credentials that could be mis-used!)
   * detailed and complete step-by-step instructions to reproduce the bug
6. Precise description:
   * precisely state the expected and the actual behavior
   * generally give as much additional information as possible. (But find the right balance: do not invest hours for a very obvious and easy to solve issue. When in doubt, give more information.)
7. Minimal example: it is highly encouraged to provide a minimal example to reproduce: 
   * isolate the application code which triggers the issue and strip it down as much as possible as long as the issue still occurs
   * if several files are required, you can create a gist
   * this may not always be possible and sometimes be overkill, but it always helps analyzing a bug
8. Only one bug per report: open different tickets for different issues

Please report bugs in English, so all users can understand them.

If the bug appears to be a regression introduced in a new version of the project, try to find the closest versions between which it was introduced and take special care to make sure the issue is not caused by your application's usage of any internal method which changed its behavior.


### Reporting Security Issues

We take security issues in our projects seriously. We appreciate your efforts to responsibly disclose your findings.

Please do not report security issues directly on GitHub but using one of the channels listed below. This allows us to provide a fix before an issue can be exploited.

- **Researchers/Non-SAP Customers:** Please consult SAPs [disclosure guidelines](https://wiki.scn.sap.com/wiki/display/PSR/Disclosure+Guidelines+for+SAP+Security+Advisories) and send the related information in a PGP encrypted e-mail to secure@sap.com. Find the public PGP key [here](https://www.sap.com/dmc/policies/pgp/keyblock.txt).
- **SAP Customers:** If the security issue is not covered by a published security note, please report it by creating a customer message at https://launchpad.support.sap.com.

Please also refer to the general [SAP security information page](https://www.sap.com/about/trust-center/security/incident-management.html).


### Issue Reporting Disclaimer

We want to improve the quality of cf-java-client-sap and good bug reports are welcome! But our capacity is limited, so we cannot handle questions or consultation requests and we cannot afford to ask for required details. So we reserve the right to close or to not process insufficient bug reports in favor of those which are very cleanly documented and easy to reproduce. Even though we would like to solve each well-documented issue, there is always the chance that it won't happen - remember: cf-java-client-sap is Open Source and comes without warranty.

Bug report analysis support is very welcome! (e.g. pre-analysis or proposing solutions)


## Contribute Code

You are welcome to contribute code to cf-java-client-sap in order to fix bugs or to implement new features.

There are three important things to know:

1.  You must be aware of the Apache License (which describes contributions) and **agree to the Developer Certificate of Origin**. This is common practice in all major Open Source projects. To make this process as simple as possible, we are using *[CLA assistant](https://cla-assistant.io/)*. CLA assistant is an open source tool that integrates with GitHub very well and enables a one-click-experience for accepting the DCO. See the respective section below for details.
2.  There are **several requirements regarding code style, quality, and product standards** which need to be met (we also have to follow them). The respective section below gives more details on the coding guidelines.
3.  **Not all proposed contributions can be accepted**. Some features may e.g. just fit a third-party add-on better. The code must fit the overall direction of cf-java-client-sap and really improve it, so there should be some "bang for the byte". For most bug fixes this is a given, but major feature implementation first need to be discussed with one of the cf-java-client-sap committers (the top 20 or more of the [Contributors List](https://github.com/SAP/cf-java-client-sap/graphs/contributors)), possibly one who touched the related code recently. The more effort you invest, the better you should clarify in advance whether the contribution fits: the best way would be to just open an enhancement ticket in the issue tracker to discuss the feature you plan to implement (make it clear you intend to contribute). We will then forward the proposal to the respective code owner, this avoids disappointment.


### Developer Certificate of Origin (DCO)

Due to legal reasons, contributors will be asked to accept a DCO before they submit the first pull request to this project. SAP uses [the standard DCO text of the Linux Foundation](https://developercertificate.org/).  
This happens in an automated fashion during the submission process: the CLA assistant tool will add a comment to the pull request. Click it to check the DCO, then accept it on the following screen. CLA assistant will save this decision for upcoming contributions.

This DCO replaces the previously used CLA ("Contributor License Agreement") as well as the "Corporate Contributor License Agreement" with new terms which are well-known standards and hence easier to approve by legal departments. Contributors who had already accepted the CLA in the past may be asked once to accept the new DCO.

### How to contribute - the Process

#### Fork the project
* To develop your contribution to the project, first [fork](https://help.github.com/articles/fork-a-repo/) this repository in your own github account. 

* When developing make sure to keep your fork up to date with the origin's master branch or the release branch you want to contribute a fix to.

#### How to build and run?
You can read how to build cf-java-client-sap [here](https://github.com/SAP/cf-java-client-sap#compiling-and-packaging).

#### Testing
* To ensure no regressions to previous functionality execute `mvn clean test` in the project's root folder to run all the unit tests.

* If you are developing new functionality make sure to add tests covering the new scenarios where applicable!

* There are internal integration tests which are not visilbe outside of SAP. When the contribution is ready someone from the commiters will execute them and verify the result.

#### Formatting
Having the same style of formatting across the project helps a lot with readability.

##### Eclipse
Our team is developing on the [Eclipse](http://www.eclipse.org/) IDE and we have a handy formatter located [here](https://github.com/cloudfoundry-incubator/multiapps/tree/master/ide). In Eclipse you can import the formatter from `Window > Preferences > Java > Code Style > Formatter`

##### IntelliJ
If you're using IntelliJ you can try the [EclipseCodeFormatter](https://github.com/krasa/EclipseCodeFormatter) plugin.

##### NetBeans
NetBeans also provides such a plugin. Just search for `eclipse formatter` in the [PluginPortal](http://plugins.netbeans.org/PluginPortal/).

#### Creating a pull request
When creating a pull request please use the provided template. Don't forget to link the [Issue](https://github.com/SAP/cf-java-client-sap/issues) if there is one related to your pull request!