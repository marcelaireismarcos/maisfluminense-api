package maisfluminense.vikkynsnorth.noticias.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class LinkSourceContent {

	private boolean success = false;
	private String htmlCode = "";
	private String raw = "";
	private String title = "";
	private String description = "";
	private String url = "";
	private String finalUrl = "";
	private String cannonicalUrl = "";
	private HashMap<String, String> metaTags = new HashMap<String, String>();

	private List<String> images = new ArrayList<String>();
	private String[] urlData = new String[2];

	public LinkSourceContent() {
	}

	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @param success
	 *            the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * @return the htmlCode
	 */
	public String getHtmlCode() {
		return htmlCode;
	}

	/**
	 * @param htmlCode
	 *            the htmlCode to set
	 */
	public void setHtmlCode(String htmlCode) {
		this.htmlCode = htmlCode;
	}

	/**
	 * @return the raw
	 */
	public String getRaw() {
		return raw;
	}

	/**
	 * @param raw
	 *            the raw to set
	 */
	public void setRaw(String raw) {
		this.raw = raw;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the finalUrl
	 */
	public String getFinalUrl() {
		return finalUrl;
	}

	/**
	 * @param finalUrl
	 *            the finalUrl to set
	 */
	public void setFinalUrl(String finalUrl) {
		this.finalUrl = finalUrl;
	}

	/**
	 * @return the cannonicalUrl
	 */
	public String getCannonicalUrl() {
		return cannonicalUrl;
	}

	/**
	 * @param cannonicalUrl
	 *            the cannonicalUrl to set
	 */
	public void setCannonicalUrl(String cannonicalUrl) {
		this.cannonicalUrl = cannonicalUrl;
	}

	/**
	 * @return the metaTags
	 */
	public HashMap<String, String> getMetaTags() {
		return metaTags;
	}

	/**
	 * @param metaTags
	 *            the metaTags to set
	 */
	public void setMetaTags(HashMap<String, String> metaTags) {
		this.metaTags = metaTags;
	}

	/**
	 * @return the images
	 */
	public List<String> getImages() {
		return images;
	}

	/**
	 * @param images
	 *            the images to set
	 */
	public void setImages(List<String> images) {
		this.images = images;
	}

	/**
	 * @return the urlData
	 */
	public String[] getUrlData() {
		return urlData;
	}

	/**
	 * @param urlData
	 *            the urlData to set
	 */
	public void setUrlData(String[] urlData) {
		this.urlData = urlData;
	}

}
