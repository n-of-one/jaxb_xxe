JAXB XXE configuration tester
-----------------------------

This project shows the effects that various flags have on XML parsing through JAXB.

The aim is to improve the suggested setup for safely parsing JAXB on the OWASP cheat sheet: 
https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxb-unmarshaller


Java & JAXB versions
--------------------
This project was tested using OpenJDK java 17 (LTS) and imports JAXB via org.glassfish.jaxb:jaxb-runtime:4.0.2


Details
-----------------
When running the main method, three files are parsed:

1. Baseline file with no XXE at all:

```xml
<person><name>test</name></person>
```
2. XML file with an expansion attack 10 laughs (because a billion laughs would not be practical)
```xml
<!DOCTYPE lolz [
<!ENTITY lol "lol">
<!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">]>
<person><name>&lol1;</name></person>
```


3. XML file with a File XXE that attempts to steal the contents of the secret.txt file that is located in the root of this project.
```xml
<!DOCTYPE filez [
<!ENTITY secret SYSTEM "file:secret.txt">]>
<person><name>&secret;</name></person>
```

The files are processed 4 times:

1. With a JAXB setup that has deliberately been misconfigured to be vulnerable
2. With a JAXB parser that has not been configured for safety. 
3. With a JAXB parser setup that has been configured according to the OWASP recommendation setup.
3. With a JAXB parser setup that has been configured using a slightly modified setup only using the `disallow-doctype-decl` flag.

Results
-------
Running this program yields the following results:
```
Baseline: jaxb unmarshalling - manually made unsafe

Normal XML parsing: safe
  result=test
  expect=test

XXE Expansion (billion laughs): unsafe
  result=lollollollollollollollollollol
  expect=(some kind of exception). (if you see lololololol... then lol was expanded)

XXE retrieve files: unsafe
  result=secret_code1234
  expect=(some kind of exception) If you see `secret_code1234` then the file was stolen

----------------------------------------
Plain JAXB unmarshalling, no features set.

Normal XML parsing: safe
  result=test
  expect=test

XXE Expansion (billion laughs): unsafe
  result=lollollollollollollollollollol
  expect=(some kind of exception). (if you see lololololol... then lol was expanded)

XXE retrieve files: safe
  result=class jakarta.xml.bind.UnmarshalException
  expect=(some kind of exception) If you see `secret_code1234` then the file was stolen

-----------------------------------------------------------
JAXB using SAXParser with OWASP recommended features

Normal XML parsing: safe
  result=test
  expect=test

XXE Expansion (billion laughs): unsafe
  result=lollollollollollollollollollol
  expect=(some kind of exception). (if you see lololololol... then lol was expanded)

XXE retrieve files: safe
  result=
  expect=(some kind of exception) If you see `secret_code1234` then the file was stolen

-------------------------------------------------------
JAXB using SAXParser with only 'disallow-doctype-decl'

Normal XML parsing: safe
  result=test
  expect=test

XXE Expansion (billion laughs): safe
  result=class jakarta.xml.bind.UnmarshalException
  expect=(some kind of exception). (if you see lololololol... then lol was expanded)

XXE retrieve files: safe
  result=class jakarta.xml.bind.UnmarshalException
  expect=(some kind of exception) If you see `secret_code1234` then the file was stolen

```

