package maisfluminense.vikkynsnorth.noticias.library;

/**
 * Callback that is invoked with before and after the loading of a link preview
 * 
 */
public interface LinkViewCallback {

	void onBeforeLoading();

	/**
	 * 
	 * @param linkSourceContent
	 *            Class with all contents from preview.
	 * @param isNull
	 *            Indicates if the content is null.
	 */
	void onAfterLoading(maisfluminense.vikkynsnorth.noticias.library.LinkSourceContent linkSourceContent, boolean isNull);
}
