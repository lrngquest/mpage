#!/usr/bin/env bb
(ns mpage  (:import java.io.File  (java.nio.file Path Files)
                    (java.time Instant ZoneId ZonedDateTime)
                    (java.time.format DateTimeFormatter) )  )


(def ps-pn (atom 0)) (def ps-op (atom 0)) (def file-x (atom {} ))
(def sheet (atom {}))

(def opt-tumble 0)    (def opt-duplex 1) ;;TODO
(def opt-tabstop 8)  (def opt-indent 0)
(def TSIZE 12)  (def fsize TSIZE)  (def Hsize (+ fsize 2))

(def ps-height 792)  (def ps-width 612);;"Letter"  TODO

(def smgn-bottom 18) (def smgn-left 18) (def smgn-right 18) (def smgn-top 18)
(def shdr-bottom  0) (def shdr-left  0) (def shdr-right  0) (def shdr-top  0)
(def pgmgn-bottom 4) (def pgmgn-left 4) (def pgmgn-right 4) (def pgmgn-top 4)

(def xbase1  (+ smgn-right shdr-right) );;now both legs of orig.==>same value!
(def ybase1  (+ smgn-bottom shdr-bottom) )
(def ytop4  (+ (- ps-height smgn-bottom smgn-top shdr-bottom shdr-top) ybase1))
(def xwid1  (- ps-width smgn-left smgn-right shdr-left shdr-right) )
(def xwid2  (/ xwid1 2))
(def xbase2 (+ xwid2 smgn-left shdr-left))
(def yht1   (- ps-height smgn-top smgn-bottom shdr-top shdr-bottom) )
(def yht2   (/ yht1 2) )  ;;used in :width
(def ybase3 (+ yht2 ybase1))
(def ytop2   ybase3)    ;;used in :pagepoints  ;;ditto ytop4

(def fontname "Courier") (def media "Letter")
(def MPAGE "mpage") (def VERSION "2.5.6 Januari 2008")
(def fmtr  (DateTimeFormatter/ofPattern "EEE MMM dd kk:mm:ss z yyyy") )

(defn ps-title "" [file-name]
  (println "%!PS-Adobe-2.0")
  (println "%%DocumentFonts:" fontname  "Times-Bold")
  (println "%%Title:" (format "%s (%s)" file-name MPAGE) )
  (println "%%Creator:" MPAGE  VERSION)
  (println "%%CreationDate:"  (.format fmtr (ZonedDateTime/now)) )
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
  (outline-1) (printf "%d %d moveto %d 0 rlineto stroke\n" xbase1 ybase3 xwid1))

(defn outline-4 "" []
  (outline-2) (printf "%d %d moveto 0 %d rlineto stroke\n" xbase2 ybase1 yht1))


(defn psb-trans-rot [ [pp-origin-y pp-origin-x]]
  (let [sh-rotate  (:rotate @sheet)  ]
    (printf "%d %d translate\n"  pp-origin-x  pp-origin-y)
    (when (not= sh-rotate 0)  (printf "%d rotate\n" sh-rotate) )  )  )


(defn psb-clip-scale [  o-m-h]
  (let [{:keys [:height :width :plength :cwidth]} @sheet
        pheight  (+ (* plength fsize) (if (> o-m-h 0)  (+ Hsize 2)  0))  ]
    (printf "0 0 moveto 0 %d rlineto %d 0 rlineto 0 %d rlineto closepath clip\n"
            height width (- 0 height))
    (printf "%d %d mp_a_x mul div %d %d div scale\n"
            width cwidth height pheight)  )   )


(defn psb-header-bar []
  (let [{:keys [:plength :cwidth]}  @sheet
        {:keys [:file-date :file-pagenum :file-name]}  @file-x
        pos0   (* plength fsize)
        pos   (+ pos0 4)   ]
    (printf "newpath 0 %d moveto %d mp_a_x mul 0 rlineto stroke\n" pos0 cwidth)
    (printf "headerfont setfont\n")
    (printf "3 %d moveto (%s) show\n"  pos file-date)
    (printf "%d mp_a_x mul dup (Page %d) stringwidth pop sub 3 sub %d moveto (Page %d) show\n"  cwidth  file-pagenum  pos  file-pagenum)
    (printf "fnamefont setfont\n")
    (printf "(%s) stringwidth pop sub 2 div %d moveto\n"  file-name  pos)
    (printf "(%s) show\n"  file-name)  )   )


(defn psb-account-for-margin []
  (let [{:keys [:width :plength]}  @sheet  ]
    (printf "%d %d translate %d %d div %d %d div scale\n"
            pgmgn-left    (+ pgmgn-bottom (/ fsize 4))
            (- width pgmgn-left pgmgn-right)
            width
            (- (* plength fsize) pgmgn-top pgmgn-bottom)
            (* plength fsize) ) )  )


(defn psb-t-onepage "" [ pl-line pl-col textA]
  (printf "%s %d moveto (%s) show\n"
            (case pl-col
              0   "0"
              1   "mp_a_x"
              (format "%d mp_a_x mul"  pl-col)  )
            (* fsize (- (:plength @sheet) pl-line))
            textA )  )


(defn is-prntbl? "space..~" [ch] (or (and (>= ch 32) (<= ch 126)) (>= ch 128)) )

(defn calnc "" [nucol]
  (+ nucol  (- opt-tabstop (mod (- nucol opt-indent) opt-tabstop) )) )

(defn mp-get-text "rewriten for bb" [pbR iln icl pbc]
  (let [[oTstr tv]          ;; tv :: [pl-line pl-col pl-nuline pl-nucol ich]
          (loop [outTsq []   ich (if (= 0 pbc) (.read pbR) pbc)   nucl icl]

            (if (and (is-prntbl? ich) (< nucl (:cwidth @sheet)) )

              (recur (if (contains? #{ \( \) \\} (char ich))
                         (conj outTsq \\ (char ich))  ;;ps-esc req'd!
                         (conj outTsq    (if (> ich 127) \X (char ich)) ) )
                     (.read pbR )
                     (inc nucl)   )
              
              [ (apply str outTsq)   ;; conv seq to string
                (cond                         ;; thus push back----v
                 (is-prntbl? ich)  [iln icl  (inc iln)  opt-indent ich]
                 (= ich -1)        (do (swap! file-x assoc :fin :FILE-EOF)
                                        [iln icl iln nucl 0] )
                 (= ich 10)        [iln icl  (inc iln)  opt-indent  0]  ;\n
                 (= ich  9)        [iln icl  iln      (calnc nucl)  0]  ;\t
                 (= ich 13)        [iln icl  iln        opt-indent  0]  ;\r
                 :else (do (println "fail!"ich) [iln icl iln icl 0]) )
                ]  )    )  ;; thus  returns [ string state-vec-of-int]
        
          outTrm   (clojure.string/triml oTstr)      ]
    
    [outTrm   (assoc tv 1  (+ (tv 1) (- (count oTstr) (count outTrm)))) ] )   )


(defn file-more? "" [] (= (:fin @file-x) :FILE-MORE))

(defn text-onepage "rewriten for bb" [pbR]
  (loop [pln 1  pcl 0  pbc 0]
    (when (and (file-more?)  (<= pln (:plength @sheet)) )
      (let [[textA  [tln tcl nuln nucl nupbc]]  (mp-get-text pbR pln pcl pbc) ]
        (when (> (count textA) 0)               (psb-t-onepage  tln tcl textA) )
        (recur nuln nucl nupbc)  ) ) )   )


(defn p-mp-outline "" []
  (case (:outline @sheet)  1 (outline-1)  2 (outline-2)  4 (outline-4)) )

(defn pgnn "ps-comment for ps-pagenum" []
  (swap! ps-pn (constantly (inc @ps-pn)))
  (println (str "%%Page: " (format "%d %d" @ps-pn @ps-pn)))   )


(def pntx (atom 1) ) ;;must defer _real_ init until outline chosen

(defn do-text-sheet  [pbR]
  (when (= 0  (((:pagepoints @sheet) @pntx) 1) ) ;; re-init sheet '-c' 1of2
    (pgnn) (println "save") (p-mp-outline)  (swap! pntx (constantly 0)) )

  (loop []
    (when (and (not= 0 (((:pagepoints @sheet) @pntx) 1))  (file-more?)  )
      (swap! file-x assoc  :file-pagenum (inc (:file-pagenum @file-x)) )
      (println "gsave")
      (psb-trans-rot ((:pagepoints @sheet) @pntx) )
      (psb-clip-scale 1)
      (psb-header-bar)
      (psb-account-for-margin)
      (println "textfont setfont")
      (text-onepage pbR)
      (println "grestore")
      (swap! pntx (constantly(inc @pntx))) ;;so to next pagepoint on sheet
      (recur)  )  )
  
  (when (= 0  (((:pagepoints @sheet) @pntx) 1) )  ;;sheet filled
    (println "restore\nshowpage") )     )


(defn do-sheets "sheets as required for a file" [pbR]
  (loop [i 0]
    (when (and (file-more?) (< i 1024))
      (do-text-sheet pbR)
      (swap! ps-op (constantly (inc @ps-op)))  (recur (inc i))
      )   ))


(defn zoned-d-t "" [ms]
  (ZonedDateTime/ofInstant (Instant/ofEpochMilli ms) (ZoneId/systemDefault)))


(defn do-file "" [file-arg]
  (let [file-obj    (File. file-arg)
        path        (.toPath file-obj)
        file-d-t    (.format fmtr (zoned-d-t (.lastModified file-obj)))    ]
    (with-open [bR   (Files/newBufferedReader path) ]    
      (swap! file-x assoc  :file-date file-d-t  :file-name (.toString path)
             :file-pagenum 0    :fin :FILE-MORE)
      (do-sheets  bR)  ) )   )


(defn -main  ""  [& args]
  (reset! sheet (   ;; initialize 'sheet' per outline-option-arg
{"-1"{:pagepoints [ [ybase1 xbase1] [0 0] ]
      :outline 1   :rotate 0
      :height yht1 :width xwid1 :plength 66 :cwidth 80} 
 "-2"{:pagepoints [ [ytop4 xbase1] [ytop2 xbase1] [0 0] ]
      :outline 2   :rotate -90
      :height xwid1 :width yht2 :plength 66 :cwidth 80}
 "-4"{:pagepoints [ [ybase3 xbase1]  [ybase1 xbase1]
                     [ybase3 xbase2]  [ybase1 xbase2] [0 0] ]
      :rotate 0   :outline 4  :height yht2  :width xwid2
      :plength 66 :cwidth 80  }  }  (first args)) )

  (reset! pntx  (dec (count (:pagepoints @sheet))) )  ;; opt '-c' 0of2
  (ps-title  (.getName (File. (second args) ) ) )
  (doseq [fnarg (rest args)]  (do-file fnarg))       ;;  v-- '-c' 2of2
  (when (not= 0 (((:pagepoints @sheet) @pntx) 1)) (println "restore\nshowpage"))
  (ps-trailer)  )

(apply -main *command-line-args*)
