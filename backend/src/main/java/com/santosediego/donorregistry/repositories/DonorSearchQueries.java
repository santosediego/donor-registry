package com.santosediego.donorregistry.repositories;

/**
 * Pieces of the donor search query, defined once. They are compile-time
 * constants, so they can be concatenated inside @Query annotations.
 *
 * Accent-insensitive search: TRANSLATE runs first (accented letters, lower and
 * upper case, to plain letters) and LOWER runs after, so the result does not
 * depend on the database locale. Both the column and the search term go through
 * the same expression.
 */
public final class DonorSearchQueries {

	public static final String ACCENTED = "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ";
	public static final String PLAIN = "aaaaaeeeeiiiiooooouuuucn" + "AAAAAEEEEIIIIOOOOOUUUUCN";

	public static final String NORMALIZED_NAME = "LOWER(TRANSLATE(d.name, '" + ACCENTED + "', '" + PLAIN + "'))";
	public static final String NORMALIZED_TERM = "LOWER(TRANSLATE(:name, '" + ACCENTED + "', '" + PLAIN + "'))";

	// Escape character for LIKE, so a typed % or _ is a literal, not a wildcard
	public static final char LIKE_ESCAPE = '!';

	private DonorSearchQueries() {
	}
}
