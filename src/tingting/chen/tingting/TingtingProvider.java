/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.tingting;

import android.content.*;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.util.TingtingUtils;

/**
 * 应用程序数据源。
 *
 * @author longkai
 */
public class TingtingProvider extends ContentProvider {

	public static final String TAG = "TingtingProvider";

	public static final String AUTHORITY = "tingting.chen";
	public static final String BASE_URI = "content://" + AUTHORITY;

	public static final String MULTIPLE_RECORDS_MIME_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".";
	public static final String SINGLE_RECORD_MIME_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".";

	public static final int USER = 0;
	public static final int USERS = 1;
	public static final int STATUS = 2;
	public static final int STATUSES = 3;

	private static UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

	static {
		matcher.addURI(AUTHORITY, User.MULTIPLE + "/#", USER);
		matcher.addURI(AUTHORITY, User.MULTIPLE, USERS);
		matcher.addURI(AUTHORITY, Status.MULTIPLE + "/#", STATUS);
		matcher.addURI(AUTHORITY, Status.MULTIPLE, STATUSES);
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
	public static Uri parse(String path, String id) {
		return Uri.parse(BASE_URI + "/" + path + "/" + id);
	}

	/**
	 * 数据源实际持有的数据库连接
	 */
	private TingtingSource mDb;

	@Override
	public boolean onCreate() {
		mDb = new TingtingSource(getContext(), "tingting.db", 1);
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
			default:
				throw new RuntimeException("unknown uri: " + uri);
		}
		SQLiteDatabase db = mDb.getWritableDatabase();
		db.beginTransaction();
		for (int i = 0; i < values.length; i++) {
			db.insertWithOnConflict(table, null, values[i], SQLiteDatabase.CONFLICT_REPLACE);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		getContext().getContentResolver().notifyChange(uri, null, false);
		return values.length;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("not supported for now!");
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("not supported for now!");
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
			Log.i(TAG, "finish create tables...");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE " + User.TABLE);
			db.execSQL("DROP TABLE " + Status.TABLE);
			Log.i(TAG, "finish upgrade table...");
		}
	}
}
