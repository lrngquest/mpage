(ns mpagebb
  (:import  java.io.File   (java.nio.file Path Files)
            (java.time Instant ZoneId LocalDateTime))    )


(def ps-pn (atom 0)) (def ps-op (atom 0))      (def file-x (atom {} ))

(def opt-tumble 0)    (def opt-duplex 1) ;;TODO
(def opt-tabstop 8)  (def opt-indent 0)  (def opt-outline 1)
(def TSIZE 12)  (def fsize TSIZE)  (def Hsize (+ fsize 2))

(def ps-height 792)  (def ps-width 612);;"Letter"  TODO

(def smgn-bottom 18) (def smgn-left 18) (def smgn-right 18) (def smgn-top 18)
(def shdr-bottom  0) (def shdr-left  0) (def shdr-right  0) (def shdr-top  0)
(def pgmgn-bottom 4) (def pgmgn-left 4) (def pgmgn-right 4) (def pgmgn-top 4)

(def xbase1  (+ smgn-right shdr-right) );;now both legs of orig.==>same value!
(def ybase1  (+ smgn-bottom shdr-bottom) )
(def ybase3
  (+ (/ (- ps-height smgn-bottom smgn-top shdr-bottom shdr-top) 2) ybase1) )
(def ytop2   ybase3)    ;;used in :sh-pagepoints  ;;ditto ytop4
(def ytop4  (+ (- ps-height smgn-bottom smgn-top shdr-bottom shdr-top) ybase1))
(def xwid1  (- ps-width smgn-left smgn-right shdr-left shdr-right) )
(def yht1   (- ps-height smgn-top smgn-bottom shdr-top shdr-bottom) )
(def yht2   (/ yht1 2) )  ;;used in :sh-width


(def zsheet {:sh-pagepoints [ [ytop4 xbase1] [ytop2 xbase1] [0 0] ]
             :sh-outline 2   :sh-rotate -90
             :sh-height xwid1 :sh-width yht2 :sh-plength 66 :sh-cwidth 80} )


(def fontname "Courier") (def media "Letter")
(def MPAGE "mpage") (def VERSION "2.5.6 Januari 2008")

(defn ps-title "" [file-name]
  (println "%!PS-Adobe-2.0")
  (println "%%DocumentFonts:" fontname  "Times-Bold")
  (println "%%Title:" (format "%s (%s)" file-name MPAGE) )
  (println "%%Creator:" MPAGE  VERSION)
  ;(println "%%CreationDate:" (.toString (Date.)) )
  (println "%%Orientation:" "Landscape") ;;TODO
  (println "%%DocumentMedia:" (format "%s %d %d" media ps-width ps-height))
  (println "%%BoundingBox:"
           (format "%d %d %d %d" smgn-left  smgn-bottom  (- ps-width smgn-right)
                   (- ps-height smgn-top)) )
  (println "%%Pages: (atend)")
  (println "%%EndComments")
  (println "%%BeginProlog")
  (println "/mp_stm usertime def")
  (println "/mp_pgc statusdict begin pagecount end def")
  (printf  "statusdict begin /jobname (%s) def end\n"   file-name)
  (when (> opt-duplex 0)
      (do (print "statusdict /setduplexmode known")
          (printf " { statusdict begin true setduplexmode end } if\n")
          (when (> opt-tumble 0)
            (do (println "statusdict /settumble known "
                         "{ statusdict begin true settumble end } if")) ) ))
;;omits if opt-encoding block     first_encoding = ' '; last_encoding= '~'; ??
  (printf "/textfont /%s findfont %d scalefont def\n"  fontname (dec fsize) )
  (printf "/fnamefont /Times-Bold findfont %d scalefont def\n"  Hsize)
  (printf "/headerfont /Times-Bold findfont %d scalefont def\n"  (- Hsize 2))
  (println "textfont setfont")
  (println  "(a) stringwidth pop /mp_a_x exch def")
  (println "%%EndProlog")
  (when (> opt-duplex 0)
      (do (println "%%BeginSetup")
          (println "%%BeginFeature: *Duplex"
                   (format "Duplex%sTumble" (if (> opt-tumble 0) "" "No")))
          (printf "<< /Duplex true /Tumble %s >> setpagedevice\n"
                  (if (> opt-tumble 0) "true" "false"))
          (println "%%EndFeature")      (println "%%EndSetup")  ))    )

(defn ps-trailer "" []
  (println "%%Trailer")
  (print   "statusdict begin jobname print flush" )
  (println " (: Job finished:\\n) print")
  (print   "(\\tmpage time (s) = ) print flush usertime ")
  (print   "mp_stm sub 1000 div ==\n(\\tmpage pages = ) print")
  (println " flush pagecount mp_pgc sub ==\nend flush")
  (println (str "%%Pages:") (format "%d" @ps-op) )    )


(defn outline-1 "" []
  (printf  "0 setlinewidth\n")
  (printf "%d %d moveto 0 %d rlineto\n"  xbase1  ybase1  yht1)
  (printf "%d 0 rlineto 0 %d rlineto closepath stroke\n" xwid1 (- 0 yht1)) )

(defn outline-2 "" []
  (outline-1 )
  (printf "%d %d moveto %d 0 rlineto stroke\n" xbase1  ybase3  xwid1)  )


(defn psb-trans-rot [ pp-v]
  (let [[pp-origin-y pp-origin-x]   pp-v
        sh-rotate  (:sh-rotate zsheet)  ]
    (printf "%d %d translate\n"  pp-origin-x  pp-origin-y)
    (when (not= sh-rotate 0)  (printf "%d rotate\n" sh-rotate) )  )  )


(defn psb-clip-scale [  o-m-h]
  (let [{:keys [:sh-pagepoints :sh-outline :sh-rotate
                :sh-height :sh-width :sh-plength :sh-cwidth]} zsheet
        pheight  (+ (* sh-plength fsize) (if (> o-m-h 0)  (+ Hsize 2)  0))  ]
    (printf "0 0 moveto 0 %d rlineto %d 0 rlineto 0 %d rlineto closepath clip\n"
            sh-height sh-width (- 0 sh-height))
    (printf "%d %d mp_a_x mul div %d %d div scale\n"
            sh-width sh-cwidth sh-height pheight)  )   )


(defn psb-header-bar []
  (let [sh-plength  (:sh-plength zsheet)    sh-cwidth  (:sh-cwidth zsheet)
        {:keys [:file-date :file-pagenum :file-name :fin]}  @file-x
        pos0   (* sh-plength fsize)
        pos   (+ pos0 4)   ]
    (printf "newpath 0 %d moveto %d mp_a_x mul 0 rlineto stroke\n"
            pos0  sh-cwidth)
    (printf "headerfont setfont\n")
    (printf "3 %d moveto (%s) show\n"  pos file-date)
    (printf "%d mp_a_x mul dup (Page %d) stringwidth pop sub 3 sub %d moveto (Page %d) show\n"  sh-cwidth  file-pagenum  pos  file-pagenum)
    (printf "fnamefont setfont\n")
    (printf "(%s) stringwidth pop sub 2 div %d moveto\n"  file-name  pos)
    (printf "(%s) show\n"  file-name)  )   )


(defn psb-account-for-margin []
  (let [sh-width (:sh-width zsheet)   sh-plength (:sh-plength zsheet)]
    (printf "%d %d translate %d %d div %d %d div scale\n"
            pgmgn-left    (+ pgmgn-bottom (/ fsize 4))
            (- sh-width pgmgn-left pgmgn-right)
            sh-width
            (- (* sh-plength fsize) pgmgn-top pgmgn-bottom)
            (* sh-plength fsize) ) )  )


(defn psb-t-onepage "" [ pl-line pl-col textA]
  (printf "%s %d moveto (%s) show\n"
            (case pl-col
              0   "0"
              1   "mp_a_x"
              (format "%d mp_a_x mul"  pl-col)  )
            (* fsize (- (:sh-plength zsheet) pl-line))
            textA )  )


(defn is-prntbl? "space..~  Excludes EOF" [ch] (and (>= ch 32) (<= ch 126)) )

(defn calnc "" [nucol]
  (+ nucol  (- opt-tabstop (mod (- nucol opt-indent) opt-tabstop) )) )

(defn mp-get-text "" [bR iline icol pbch]
  (let [sh-cwidth  (:sh-cwidth zsheet)
        [oTstr tv]          ;; tv ==> [pl-line pl-col pl-newline pl-newcol]
          (loop [outTsq []  ichr (if (= 0 pbch) (.read bR) pbch)  plnewcol icol]
            (if (and (is-prntbl? ichr) (< plnewcol sh-cwidth))

              (recur (if (contains? #{ \( \) \\} (char ichr))
                         (conj outTsq \\ (char ichr)) ;;ps-esc req'd!
                         (conj outTsq    (char ichr)))
                       (.read bR )
                       (inc plnewcol) )
              
              [ (apply str outTsq)   ;; thus conv seq to string
                (cond            ;; thus push back 'ichr'           ----v
                 (is-prntbl? ichr)  [iline icol  (inc iline) opt-indent ichr]
                 (= ichr -1)        (do (swap! file-x assoc :fin :FILE-EOF)
                                        [iline icol iline plnewcol 0])
                 (= ichr 10)        [iline icol  (inc iline) opt-indent 0];\n
                 (= ichr  9)        [iline icol iline  (calnc plnewcol) 0] ;\t
                 (= ichr 13)        [iline icol iline  opt-indent 0] ;\r
                 :else
                 (do (println "fail!"ichr) [iline icol iline icol 0]) )
                ]    )    )  ;; thus  returns [ string state-vec-of-int]
        
        outTrm   (clojure.string/triml oTstr)
        ctTrm    (count outTrm)   ];; pl-col aka (tv 1) per blanks trimmed
    [outTrm  (assoc tv 1  (+ (tv 1) (- (count oTstr) ctTrm))) ] )    )


(defn file-more? "" [] (= (:fin @file-x) :FILE-MORE))

(defn text-onepage "" [bR]
  (loop [pline 1  pcol 0  pbc 0]
    (if (and (file-more?)  (<= pline (:sh-plength zsheet)) )
      (let [[textA  tv]                   (mp-get-text bR pline pcol pbc )
            [tline tcol  tnuline tnucol tnpbc]   tv  ]
        (when (> (count textA) 0)  (psb-t-onepage  tline tcol textA) )
        (recur tnuline tnucol tnpbc)  ) ;; ala (swap!...) ;;then
      0)
    )  )


(defn p-mp-outline "s/b from sheet" [] (when (> opt-outline 0) (outline-2))  )

(defn pgnn "ps-comment for ps-pagenum" []
  (swap! ps-pn (constantly (inc @ps-pn)))
  (println (str "%%Page: " (format "%d %d" @ps-pn @ps-pn)))   )


(def pntx (atom (dec (count (:sh-pagepoints zsheet)))) )

(defn do-text-sheet "sheet of page(s) per pagepoints" [bR]
  (when (= 0  (((:sh-pagepoints zsheet) @pntx) 1) ) ;;"full" re-init sheet
    (do (pgnn) (println "save") (p-mp-outline)  (swap! pntx (constantly 0))))
  
  (loop []
    (if (and (not= 0 (((:sh-pagepoints zsheet) @pntx) 1))  (file-more?)  )
      (do  (swap! file-x assoc  :file-pagenum (inc (:file-pagenum @file-x)) )
           (println "gsave")
           (psb-trans-rot ((:sh-pagepoints zsheet) @pntx) )
           (psb-clip-scale 1)
           (psb-header-bar)
           (psb-account-for-margin)
           (println "textfont setfont")
           (text-onepage bR)
           (println "grestore")
           (swap! pntx (constantly(inc @pntx))) ;;so to next pagepoint on sheet
           (recur) )
      nil)  )
  
  (when (= 0  (((:sh-pagepoints zsheet) @pntx) 1) )  ;;sheet filled
    (do (println "restore") (println "showpage")))     )


(defn do-sheets "sheets as required for a file" [bR]
  (loop [i 0]
    (if (and (file-more?) (< i 1024))
      (do (do-text-sheet bR)
          (swap! ps-op (constantly (inc @ps-op)))     (recur  (inc i)) )
      i )    ))


(defn local-date-time "" [millisec] ;;bb ZoneId/systemDefault ==> ...LosAngeles
  (let [instant  (Instant/ofEpochMilli millisec) ]
    (LocalDateTime/ofInstant instant (ZoneId/of "America/Chicago" )) ) )

(defn do-file "" [file-arg]
  (let [file-obj    (File. file-arg)
        path        (.toPath file-obj)
        file-date   (.toString( local-date-time (.lastModified file-obj)))
        bR          (Files/newBufferedReader path)        ]
    (swap! file-x assoc
         :file-date file-date  :file-name (.toString path)
         :file-pagenum 0    :fin :FILE-MORE)
    
    (do-sheets  bR)
    (.close bR)   )   )


(defn -main  ""  [& args]
  (ps-title  (.getName (io/File. (first args) ) ) )
  (doseq [arg args]  (do-file arg))
  (when (not= 0 (((:sh-pagepoints zsheet) @pntx) 1)) ;;assume '-c' !
    (do (println "restore") (println "showpage")) )
  (ps-trailer)  )

(apply -main *command-line-args*) ;;ala 'tree.clj'
