package maisfluminense.vikkynsnorth.noticias.library;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class Utils {

	public static final int ALL = 0;
	public static final int FIRST = 1;

	public static String pregMatch(String content, String pattern, int index) {

		String match = "";
		Matcher matcher = Pattern.compile(pattern).matcher(content);

		while (matcher.find()) {
			match = matcher.group(index);
			break;
		}

		return maisfluminense.vikkynsnorth.noticias.library.TextCrawler.extendedTrim(match);
	}

	public static List<String> pregMatchAll(String content, String pattern,
			int index) {

		List<String> matches = new ArrayList<String>();
		Matcher matcher = Pattern.compile(pattern).matcher(content);

		while (matcher.find()) {
			matches.add(maisfluminense.vikkynsnorth.noticias.library.TextCrawler.extendedTrim(matcher.group(index)));
		}

		return matches;
	}

	public static List<String> pregMatchAllImages(String content, String pattern) {

		List<String> matches = new ArrayList<String>();
		Matcher matcher = Pattern.compile(pattern).matcher(content);

		while (matcher.find()) {
			matches.add(maisfluminense.vikkynsnorth.noticias.library.TextCrawler.extendedTrim(matcher.group(3))
					+ matcher.group(4));
		}

		return matches;
	}

	public static List<String> pregMatchAllExtraImages(String content,
			String pattern) {

		List<String> matches = new ArrayList<String>();
		Matcher matcher = Pattern.compile(pattern).matcher(content);

		while (matcher.find()) {
			matches.add(maisfluminense.vikkynsnorth.noticias.library.TextCrawler.extendedTrim(matcher.group(3))
					+ matcher.group(4));
		}

		return matches;
	}



	/** It finds urls inside the text and return the matched ones */
	public static ArrayList<String> matches(String text) {
		return matches(text, ALL);
	}

	/** It finds urls inside the text and return the matched ones */
	public static ArrayList<String> matches(String text, int results) {

		ArrayList<String> urls = new ArrayList<String>();

		String[] splitString = (text.split(" "));
		for (String string : splitString) {

			try {
				URL item = new URL(string);
				urls.add(item.toString());
			} catch (Exception e) {
			}

			if (results == FIRST && urls.size() > 0)
				break;
		}

		return urls;
	}



}
