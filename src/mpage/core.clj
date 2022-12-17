(ns mpage.core
  (:import (java.io File FileReader BufferedReader PushbackReader)
           (java.util Date))
  (:gen-class))


(def ps-pg (atom {:ps-pagenum 0  :ps-outpages 0}))
(def file-x (atom {} ))    (def l-pl (atom {}))

(def opt-tumble 0)    (def opt-duplex 1) ;;TODO
(def opt-tabstop 8)  (def opt-indent 0)  (def opt-outline 1)

(def TSIZE 12)  (def fsize TSIZE)  (def Hsize (+ fsize 2))
(def pagemargin-bottom 4) (def pagemargin-left 4)
(def pagemargin-right 4)  (def pagemargin-top 4)
(def sheetmargin-bottom 18) (def sheetmargin-left 18) ;;points i.e. 0.25in
(def sheetmargin-right  18) (def sheetmargin-top  18)


(declare outline-2)
(def zsheet {:sh-pagepoints [{:skip 0 :pp-origin-y 774 :pp-origin-x 18}
                             {:skip 0 :pp-origin-y 396 :pp-origin-x 18}]
             :sh-outline outline-2   :sh-rotate -90
             :sh-height 576 :sh-width 378 :sh-plength 66 :sh-cwidth 80} )


(def fontname "Courier") (def media "Letter")
(def MPAGE "mpage") (def VERSION "2.5.6 Januari 2008")
 (def ps-height 792)  (def ps-width 612);;"Letter"  TODO

(defn ps-title "" [file-name]
  (println "%!PS-Adobe-2.0")
  (println "%%DocumentFonts:" fontname  "Times-Bold")
  (println "%%Title:" (format "%s (%s)" file-name MPAGE) )
  (println "%%Creator:" MPAGE  VERSION)
  (println "%%CreationDate:" (.toString (Date.)) )
  (println "%%Orientation:" "Landscape") ;;TODO
  (println "%%DocumentMedia:" (format "%s %d %d" media ps-width ps-height))
  (println "%%BoundingBox:"
           (format "%d %d %d %d"
                   sheetmargin-left sheetmargin-bottom
                   (- ps-width sheetmargin-right)
                   (- ps-height sheetmargin-top)) )
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
  (println (str "%%Pages:") (format "%d" (:ps-outpages @ps-pg)) )    )


(def sheetheader-bottom 0) (def sheetheader-left 0);;ALL 0! Simplify??  TODO
(def sheetheader-right  0) (def sheetheader-top  0)

(defn xbase1 []
  (if (= 0 (mod (:ps-pagenum @ps-pg) 2)) ;;assume opt-duplex:TRUE
    (+ sheetmargin-right sheetheader-right)
    (+ sheetmargin-left  sheetheader-left) )  )

(defn ybase1 [] (+ sheetmargin-bottom sheetheader-bottom) )
(defn ybase3 []
  (+ (/ (- ps-height sheetmargin-bottom sheetmargin-top
           sheetheader-bottom sheetheader-top )  2)
     (ybase1)))
  ;; ytop2  ytop4
(defn xwid1  [] (- ps-width sheetmargin-left sheetmargin-right
                   sheetheader-left sheetheader-right) )
(defn yht1   [] (- ps-height sheetmargin-top sheetmargin-bottom
                   sheetheader-top sheetheader-bottom))
  ;; yht2
(defn outline-1 "" []
  (printf  "0 setlinewidth\n")
  (printf "%d %d moveto 0 %d rlineto\n"  (xbase1) (ybase1) (yht1))
  (printf "%d 0 rlineto 0 %d rlineto closepath stroke\n" (xwid1) (- 0 (yht1))) )

(defn outline-2 "" []
  (outline-1 )
  (printf "%d %d moveto %d 0 rlineto stroke\n" (xbase1) (ybase3) (xwid1))  )


(defn psb-trans-rot [ pagepoint]
  (let [{:keys [skip pp-origin-y pp-origin-x]}  pagepoint
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
            pagemargin-left    (+ pagemargin-bottom (/ fsize 4))
            (- sh-width pagemargin-left pagemargin-right)
            sh-width
            (- (* sh-plength fsize) pagemargin-top pagemargin-bottom)
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

(defn tabcnt "" []
  (- opt-tabstop (mod (- (:pl-new-col @l-pl) opt-indent) opt-tabstop) ) )

(defn nxln "" []  (swap! l-pl assoc :pl-new-line (inc (:pl-new-line @l-pl))
                         :pl-new-col opt-indent))

(defn mp-get-text "" [pbR]
  (swap! l-pl assoc :pl-new-line (:pl-line @l-pl)  :pl-new-col (:pl-col @l-pl))
  (let [sh-cwidth  (:sh-cwidth zsheet)
        oTstr  (loop [outTsq []   ichr (.read pbR) ]
                 (if (and (is-prntbl? ichr) (< (:pl-new-col @l-pl) sh-cwidth))
                   (do  ;;recur -- printable && fits ==> add char(s) to out-seq
                     (swap! l-pl assoc :pl-new-col  (inc (:pl-new-col @l-pl)))
                     (recur (if (contains? #{ \( \) \\} (char ichr))
                             (conj outTsq \\ (char ichr)) ;;ps-esc req'd!
                             (conj outTsq    (char ichr)))
                           (.read pbR )) )

                   ;;exit -- fold,unprintable ==> only update l-pl
                   (do (when (is-prntbl? ichr)  (.unread pbR ichr)  (nxln) )
                       (when (= ichr -1)  (swap! file-x assoc :fin :FILE-EOF))
                       (when (= ichr 10)  (nxln) ) ;; \n
                       (when (= ichr  9) ;; \t
                              (swap! l-pl assoc  :pl-new-col
                                     (+ (:pl-new-col @l-pl) (tabcnt))))
                       (apply str outTsq) ) )  ) ;;value: seq conv'd to string
        outTrm   (clojure.string/triml oTstr)
        ctTrm    (count outTrm)  ]
    (swap! l-pl assoc :pl-col (+ (:pl-col @l-pl) (- (count oTstr) ctTrm )) )
    outTrm )    )

(defn file-more? "" [] (= (:fin @file-x) :FILE-MORE))

(defn text-onepage "" [pbR]
  (swap! l-pl assoc :pl-line 1)
  (loop []
    (if (and (file-more?)  (<= (:pl-line @l-pl) (:sh-plength zsheet)))
      (let [textA   (mp-get-text pbR)
            {:keys [:pl-line :pl-col  :pl-new-line :pl-new-col]}  @l-pl   ]
        (when (> (count textA) 0)  (psb-t-onepage  pl-line pl-col textA) )
        (swap! l-pl assoc  :pl-line pl-new-line  :pl-col pl-new-col)
        (recur)  )  ;; "then"
      0 )  )    )


(defn p-mp-outline "s/b from sheet" [] (when (> opt-outline 0) (outline-2))  )

(defn pgnn "ps-comment for ps-pagenum" []
  (let [ps-pagenum  (inc (:ps-pagenum @ps-pg)) ]
    (swap! ps-pg assoc :ps-pagenum ps-pagenum)
    (println (str "%%Page: " (format "%d %d" ps-pagenum ps-pagenum)))  )   )

(defn do-text-sheet "sheet of page(s) per pagepoints" [pbR]
  (pgnn)
  (println "save")  ;; v--- files "Attempting to call unbound fn..."
  (p-mp-outline) ;;(when (> opt-outline 0) ((:sh-outline zsheet))) ;;
  (loop [i 0]
    (if (and (file-more?) (< i (count (:sh-pagepoints zsheet) )))
      (do  (swap! file-x assoc  :file-pagenum (inc (:file-pagenum @file-x)) )
        (println "gsave")
        (psb-trans-rot  ((:sh-pagepoints zsheet)i))
        (psb-clip-scale 1)
        (psb-header-bar)
        (psb-account-for-margin)
        (println "textfont setfont")
        (text-onepage pbR)
        (println "grestore")
        (recur    (inc i) ))
      i)  )
  (println "restore")  (println "showpage")    )


(defn do-sheets "sheets as required for a file" [pbR]
  (loop [i 0]
    (if (and (file-more?) (< i 1024))
      (do (do-text-sheet pbR)
          (swap! ps-pg assoc :ps-outpages (inc (:ps-outpages @ps-pg)))
          (recur  (inc i)) )
      i )    ))


(defn do-file "" [file-arg]
  (let [file-obj   (File. file-arg)
        pbR        (->> (FileReader. file-obj)
                        (BufferedReader.  )
                        (PushbackReader.  ))   ]
    (swap! file-x assoc
         :file-date (.toString (Date. (.lastModified file-obj) ) )
         :file-name (.getName file-obj)   :file-pagenum 0    :fin :FILE-MORE)
    (swap! l-pl assoc :pl-line 0  :pl-col 0  :pl-new-line 0  :pl-new-col 0)
    
    (do-sheets  pbR)
    (.close pbR) )   )


(defn -main  ""  [& args]
  (ps-title  (.getName (File. (first args) ) ) )
  (doseq [arg args]  (do-file arg))
  (ps-trailer)  )
