package maisfluminense.vikkynsnorth.noticias.library;

/**
 * Created by chandrasekar on 04/04/16.

    Copyright (c) 2015 Chandrasekar K

        Permission is hereby granted, free of charge, to any person obtaining
        a copy of this software and associated documentation files (the
        "Software"), to deal in the Software without restriction, including
        without limitation the rights to use, copy, modify, merge, publish,
        distribute, sublicense, and/or sell copies of the Software, and to
        permit persons to whom the Software is furnished to do so without any conditions and terms.
*/
    public class Constants {

    public static final String IMAGE_PATTERN = "(.+?)\\.(jpg|png|gif|bmp)$";
    public static final String IMAGE_TAG_PATTERN = "<img(.*?)src=(\"|')(.+?)(gif|jpg|png|bmp)(\"|')(.*?)(/)?>(</img>)?";
    public static final String ICON_TAG_PATTERN = "<img(.*?)src=(\"|')(.+?)(gif|jpg|png|bmp)(\"|')(.*?)(/)?>(</img>)?";
    public static final String ICON_REV_TAG_PATTERN = "<img(.*?)src=(\"|')(.+?)(gif|jpg|png|bmp)(\"|')(.*?)(/)?>(</img>)?";
    public static final String ITEMPROP_IMAGE_TAG_PATTERN = "<img(.*?)src=(\"|')(.+?)(gif|jpg|png|bmp)(\"|')(.*?)(/)?>(</img>)?";
    public static final String ITEMPROP_IMAGE_REV_TAG_PATTERN = "<img(.*?)src=(\"|')(.+?)(gif|jpg|png|bmp)(\"|')(.*?)(/)?>(</img>)?";
    public static final String TITLE_PATTERN = "<title(.*?)>(.*?)</title>";
    public static final String SCRIPT_PATTERN = "<script(.*?)>(.*?)</script>";
    public static final String METATAG_PATTERN = "<meta(.*?)>";
    public static final String METATAG_CONTENT_PATTERN = "content=\"(.*?)\"";
    public static final String URL_PATTERN = "<\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>";


    public static final String URL_ZAPPSHARE = "https://goo.gl/l6ffgm";
}
