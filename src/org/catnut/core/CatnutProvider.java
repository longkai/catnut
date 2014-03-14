/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.core;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import org.catnut.metadata.Draft;
import org.catnut.metadata.Photo;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;

import static org.catnut.core.CatnutProvider.DRAFT;
import static org.catnut.core.CatnutProvider.DRAFTS;
import static org.catnut.core.CatnutProvider.PHOTOS;
import static org.catnut.core.CatnutProvider.STATUSES;
import static org.catnut.core.CatnutProvider.USERS;

/**
 * 应用程序数据源。
 *
 * @author longkai
 */
public class CatnutProvider extends ContentProvider {

	public static final String TAG = "CatnutProvider";

	public static final String AUTHORITY = "org.catnut";
	public static final String BASE_URI = "content://" + AUTHORITY;

	public static final String MULTIPLE_RECORDS_MIME_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".";
	public static final String SINGLE_RECORD_MIME_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".";

	public static final int USER = 0;
	public static final int USERS = 1;
	public static final int STATUS = 2;
	public static final int STATUSES = 3;
	public static final int DRAFT = 4;
	public static final int DRAFTS = 5;
	public static final int PHOTO = 6;
	public static final int PHOTOS = 7;

	private static UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

	static {
		matcher.addURI(AUTHORITY, User.MULTIPLE + "/#", USER);
		matcher.addURI(AUTHORITY, User.MULTIPLE, USERS);
		matcher.addURI(AUTHORITY, Status.MULTIPLE + "/#", STATUS);
		matcher.addURI(AUTHORITY, Status.MULTIPLE, STATUSES);
		matcher.addURI(AUTHORITY, Draft.SINGLE, DRAFT);
		matcher.addURI(AUTHORITY, Draft.MULTIPLE, DRAFTS);
		matcher.addURI(AUTHORITY, Photo.SINGLE, PHOTO);
		matcher.addURI(AUTHORITY, Photo.MULTIPLE, PHOTOS);
	}

	/**
	 * 列表uri，如http://example.org/resources
	 *
	 * @param path 如 resources
	 * @return Uri
	 */
	public static Uri parse(String path) {
		return Uri.parse(BASE_URI + "/" + path);
	}

	/**
	 * 单个对象uri，如http://example.org/resources/1
	 *
	 * @param path 如 resources
	 * @param id
	 * @return Uri
	 */
	public static Uri parse(String path, long id) {
		return Uri.parse(BASE_URI + "/" + path + "/" + id);
	}

	/**
	 * 数据源实际持有的数据库连接
	 */
	private TingtingSource mDb;

	@Override
	public boolean onCreate() {
		mDb = new TingtingSource(getContext(), "catnut.db", 1);
		return true;
	}

	@Override
	public String getType(Uri uri) {
		String type = null;
		switch (matcher.match(uri)) {
			case USER:
				type = SINGLE_RECORD_MIME_TYPE + User.SINGLE;
				break;
			case USERS:
				type = MULTIPLE_RECORDS_MIME_TYPE + User.MULTIPLE;
				break;
			case STATUS:
				type = SINGLE_RECORD_MIME_TYPE + Status.SINGLE;
				break;
			case STATUSES:
				type = MULTIPLE_RECORDS_MIME_TYPE + Status.MULTIPLE;
				break;
			case DRAFT:
				type = SINGLE_RECORD_MIME_TYPE + Draft.SINGLE;
				break;
			case DRAFTS:
				type = MULTIPLE_RECORDS_MIME_TYPE + Draft.MULTIPLE;
				break;
			case PHOTO:
				type = SINGLE_RECORD_MIME_TYPE + Photo.SINGLE;
				break;
			case PHOTOS:
				type = MULTIPLE_RECORDS_MIME_TYPE + Photo.MULTIPLE;
				break;
			default:
				Log.wtf(TAG, "unknown uri: " + uri);
		}
		return type;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDb.getReadableDatabase();
		Cursor cursor;
		switch (matcher.match(uri)) {
			case USER:
				cursor = db.query(User.TABLE, projection, queryWithId(uri), selectionArgs, null, null, null);
				break;
			case USERS:
				cursor = db.rawQuery(selection, selectionArgs);
//				cursor = db.query(User.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case STATUS:
				cursor = db.query(Status.TABLE, projection, queryWithId(uri), selectionArgs, null, null, null);
				break;
			case STATUSES:
				// 这里，比较特殊，直接在selection中写sql，可以包含占位符
				cursor = db.rawQuery(selection, selectionArgs);
				break;
			case DRAFT:
			case DRAFTS:
				cursor = db.query(Draft.TABLE, projection, selection, selectionArgs, selection, null, sortOrder);
				break;
			case PHOTO:
			case PHOTOS:
				cursor = db.rawQuery(selection, selectionArgs);
				break;
			default:
				Log.wtf(TAG, "unknown uri: " + uri);
				return null;
		}
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table;
		switch (matcher.match(uri)) {
			case USERS:
				table = User.TABLE;
				break;
			case STATUSES:
				table = Status.TABLE;
				break;
			case DRAFTS:
				table = Draft.TABLE;
				break;
			case PHOTOS:
				table = Photo.TABLE;
				break;
			default:
				throw new RuntimeException("unknown uri: " + uri);
		}
		SQLiteDatabase db = mDb.getWritableDatabase();
		long id = db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		getContext().getContentResolver().notifyChange(uri, null, false); // no syncAdapter
		return ContentUris.withAppendedId(uri, id);
	}

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		String table;
		switch (matcher.match(uri)) {
			case USERS:
				table = User.TABLE;
				break;
			case STATUSES:
				table = Status.TABLE;
				break;
			case PHOTOS:
				table = Photo.TABLE;
				break;
			default:
				throw new RuntimeException("unknown uri: " + uri);
		}
		SQLiteDatabase db = mDb.getWritableDatabase();
		db.beginTransaction();
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				db.insertWithOnConflict(table, null, values[i], SQLiteDatabase.CONFLICT_REPLACE);
			}
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		getContext().getContentResolver().notifyChange(uri, null, false);
		return values.length;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count;
		switch (matcher.match(uri)) {
			case STATUSES:
				count = mDb.getWritableDatabase().delete(Status.TABLE, selection, selectionArgs);
				break;
			case DRAFTS:
				count = mDb.getWritableDatabase().delete(Draft.TABLE, selection, selectionArgs);
				break;
			case PHOTOS:
				count = mDb.getWritableDatabase().delete(Photo.TABLE, selection, selectionArgs);
				break;
			default:
				throw new UnsupportedOperationException("not supported for now!");
		}
		getContext().getContentResolver().notifyChange(uri, null, false);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db;
		switch (matcher.match(uri)) {
			case USERS:
			case STATUSES:
				db = mDb.getWritableDatabase();
				db.execSQL(selection);
				break;
			default:
				throw new UnsupportedOperationException("not supported for now!");
		}
		getContext().getContentResolver().notifyChange(uri, null, false);
		return 1; // 随便填的
	}

	/**
	 * 直接以id查询记录
	 *
	 * @param uri
	 * @return "_id=?"
	 */
	private static String queryWithId(Uri uri) {
		return "_id=" + uri.getLastPathSegment();
	}

	/**
	 * sqlite数据库
	 *
	 * @author longkai
	 */
	private static class TingtingSource extends SQLiteOpenHelper {

		private static final String TAG = "TingtingSource";

		public TingtingSource(Context context, String name, int version) {
			super(context, name, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(User.METADATA.ddl());
			db.execSQL(Status.METADATA.ddl());
			db.execSQL(Draft.METADATA.ddl());
			db.execSQL(Photo.METADATA.ddl());
			Log.i(TAG, "finish create tables...");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE " + User.TABLE);
			db.execSQL("DROP TABLE " + Status.TABLE);
			db.execSQL("DROP TABLE " + Draft.TABLE);
			db.execSQL("DROP TABLE " + Photo.TABLE);
			Log.i(TAG, "finish upgrade table...");
		}
	}
}
