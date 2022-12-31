# mpage

From the original version:
```
  mpage:   a program to reduce pages of print so that several pages
           of output appear on one printed page.
```
Rebuilt as a Clojure learning exercise, but also to be fast enough for everyday CLI use.


## Background
Versions I have used over the years:
- original C version distributed via Usenet newsgroup, 1994
- original transpiled from MIPS executable to Java classes via XWT,  2006
- manually translated to Java (opts  `-2 -t -f -v -H `  August 2012
- rebuilt in Clojure 2019..2022


## Credits

The reimplementation is based on logic of the original C source  2.5.7 :
```
  (c) 1994-2004  Marcel J. E. Mol
  (c) 1988       Mark P. Hahn
  Released under GNU General Public License  version 2.
```

Many thanks to Michiel Borkent for fast startup  `bb`  which makes running CLI programs in Clojure such a pleasure!

   [babashka](https://github.com/babashka/babashka)


## Usage

## Examples

On Unix-like systems we use the  `#!/usr/bin/env bb`  shell feature to run the Clojure source via  `bb`  as if it were a normal executable.  If `mpage` is somewhere on your path, and marked executable, then:

```
   $ mpage  mpage.clj    >mpage.ps  # (default) 2 pages/sheet

   $ mpage -4 mpage.clj  >mpage.ps  # 4 pages/sheet
```

### Limitations

- paper size: US Letter 
- single option is pages/sheet: -1  -2 (default)  -4
- implicit options: -t -H -c
- only ASCII input rendered -- chars > 127 shown as `X`
  (original used code page ISO-8859)
- no  other options

### Bugs

None known.

## License

Other than credited above:
Copyright © 2019-2022   L. E. Vandergriff

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

   ```
"Freely you have received, freely give."  Mt. 10:8
   ```