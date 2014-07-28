CodeImpPlugin
Author: Chu Xuan Khoi
Email: chuxuankhoi@gmail.com
Version: 0.0.1

Description:
Eclipse plug-in which help Java developers in finding potential refactoring actions to improve design and code quality.

Current release description:
The current release supports the following issues:
- Inheritance issue: too many duplicated functions in classes which are the children of 1 class.

Installation procedure:
1. Copy the all file in ./CodeImpPlugin/release to [Eclipse folder]/dropins
2. Restart Eclipse

Testing procedure:
1. Open codeimp.test.all.GivenClass or any class that contains GivenClass (extends from GivenClass like Child1, Child2, ...)
2. Press "Code Improvement" button on tool-bar or select Code Improvement/Code Improvement on Menu bar.
3. One dialogue notifying that it is not the last version of plug-in appears. Press OK to skip this.
4. Wait until plug-in completes analysing the given class. You can cancel the analysing any time you want.
5. A dialogue appears to show the potential action (or notify that not potential action is detected).
6. Select 1 action and press "Next".
7. A dialogue appears to show the item to be refactored. Press "Finish" to start refactoring. After this step, user follows the normal refactoring provided by Eclipse (http://www.ibm.com/developerworks/library/os-ecref/)
