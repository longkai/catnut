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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import org.catnut.R;
import org.catnut.metadata.Comment;
import org.catnut.metadata.Draft;
import org.catnut.metadata.Photo;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.plugin.zhihu.Zhihu;
import org.catnut.util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 应用程序数据源。
 *
 * @author longkai
 */
public class CatnutProvider extends ContentProvider {

	public static final String TAG = "CatnutProvider";
	private static final String CLEAR_DATA = "clear";

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
	public static final int COMMENT = 8;
	public static final int COMMENTS = 9;

	// plugins...starting
	public static final int ZHIHU = 20;
	public static final int ZHIHUS = 21;
	// plugins...ending

	public static final int CLEAR = 100;

	private static UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

	static {
		matcher.addURI(AUTHORITY, User.MULTIPLE + "/#", USER);
		matcher.addURI(AUTHORITY, User.MULTIPLE, USERS);
		matcher.addURI(AUTHORITY, Status.MULTIPLE + "/#", STATUS);
		matcher.addURI(AUTHORITY, Status.MULTIPLE, STATUSES);
		matcher.addURI(AUTHORITY, Draft.SINGLE + "/#", DRAFT);
		matcher.addURI(AUTHORITY, Draft.MULTIPLE, DRAFTS);
		matcher.addURI(AUTHORITY, Photo.SINGLE + "/#", PHOTO);
		matcher.addURI(AUTHORITY, Photo.MULTIPLE, PHOTOS);
		matcher.addURI(AUTHORITY, Comment.SINGLE + "/#", COMMENT);
		matcher.addURI(AUTHORITY, Comment.MULTIPLE, COMMENTS);

		// plugins...starting
		matcher.addURI(AUTHORITY, Zhihu.SINGLE + "/#", ZHIHU);
		matcher.addURI(AUTHORITY, Zhihu.MULTIPLE, ZHIHUS);
		// plugins...ending

		matcher.addURI(AUTHORITY, CLEAR_DATA, CLEAR);
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
	 * 清除缓存数据
	 *
	 * @return uri
	 */
	public static Uri clear() {
		return Uri.parse(BASE_URI + File.separator + CLEAR_DATA);
	}

	/**
	 * 数据源实际持有的数据库连接
	 */
	private TingtingSource mDb;

	@Override
	public boolean onCreate() {
		mDb = new TingtingSource(getContext(), "catnut.db", 2);
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
			case COMMENT:
				type = SINGLE_RECORD_MIME_TYPE + Comment.SINGLE;
				break;
			case COMMENTS:
				type = MULTIPLE_RECORDS_MIME_TYPE + Comment.MULTIPLE;
				break;
			// plugins...starting
			case ZHIHU:
				type = SINGLE_RECORD_MIME_TYPE + Zhihu.SINGLE;
				break;
			case ZHIHUS:
				type = MULTIPLE_RECORDS_MIME_TYPE + Zhihu.MULTIPLE;
				break;
			// plugins...ending
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
			case COMMENT:
				// fall through currently
			case COMMENTS:
				cursor = db.rawQuery(selection, selectionArgs);
				break;
			// plugins...starting
			case ZHIHU:
				cursor = db.query(Zhihu.TABLE, projection, queryWithId(uri), selectionArgs, null, null, sortOrder);
				break;
			case ZHIHUS:
				cursor = db.query(Zhihu.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			// plugins...ending
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
			case COMMENTS:
				table = Comment.TABLE;
				break;
			// plugins...starting
			case ZHIHUS:
				table = Zhihu.TABLE;
				break;
			// plugins...ending
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
		int conflicOption = SQLiteDatabase.CONFLICT_REPLACE;
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
			case COMMENTS:
				table = Comment.TABLE;
				break;
			// plugins...starting
			case ZHIHUS:
				table = Zhihu.TABLE;
				conflicOption = SQLiteDatabase.CONFLICT_IGNORE;
				break;
			// plugins...ending
			default:
				throw new RuntimeException("unknown uri: " + uri);
		}
		SQLiteDatabase db = mDb.getWritableDatabase();
		db.beginTransaction();
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				db.insertWithOnConflict(table, null, values[i], conflicOption);
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
			case COMMENTS:
				count = mDb.getWritableDatabase().delete(Comment.TABLE, selection, selectionArgs);
				break;
			// plugins...starting
			case ZHIHUS:
				// 直接清掉全部
				count = mDb.getWritableDatabase().delete(Zhihu.TABLE, null, null);
				break;
			// plugins...ending
			case CLEAR: // 插件的缓存清除分开搞
				SQLiteDatabase db = mDb.getWritableDatabase();
				db.beginTransaction();
				db.execSQL("delete from " + Photo.TABLE);
				db.execSQL("delete from " + User.TABLE + " where "
						+ BaseColumns._ID + " != " + CatnutApp.getTingtingApp().getAccessToken().uid);
				db.execSQL("delete from " + Status.TABLE);
				db.execSQL("delete from " + Comment.TABLE);
				db.setTransactionSuccessful();
				db.endTransaction();
				count = Integer.MAX_VALUE;
				break;
			default:
				throw new UnsupportedOperationException("not supported for now!");
		}
		getContext().getContentResolver().notifyChange(uri, null, false);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = 1; // 随便填的
		SQLiteDatabase db;
		switch (matcher.match(uri)) {
			case USERS:
			case STATUSES:
				db = mDb.getWritableDatabase();
				db.execSQL(selection);
				break;
			// plugins...starting
			case ZHIHU:
				count = mDb.getWritableDatabase().update(Zhihu.TABLE, values, selection, selectionArgs);
				break;
			// plugins...ending
			default:
				throw new UnsupportedOperationException("not supported for now!");
		}
		getContext().getContentResolver().notifyChange(uri, null, false);
		return count;
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

		private Context mContext;

		public TingtingSource(Context context, String name, int version) {
			super(context, name, null, version);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(User.METADATA.ddl());
			db.execSQL(Status.METADATA.ddl());
			db.execSQL(Draft.METADATA.ddl());
			db.execSQL(Photo.METADATA.ddl());
			db.execSQL(Comment.METADATA.ddl());
			// plugins...starting
			db.execSQL(Zhihu.METADATA.ddl());
			// plugins...ending

			// 保存分享的图片
			(new Thread(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);
					FileOutputStream shareImage = null;
					try {
						shareImage = new FileOutputStream(mContext.getExternalCacheDir() + File.separator + Constants.SHARE_IMAGE);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, shareImage);
					} catch (FileNotFoundException e) {
					} finally {
						if (shareImage != null) {
							try {
								shareImage.close();
							} catch (IOException e) {
							}
						}
					}
				}
			})).start();
			Log.i(TAG, "finish create tables...");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "drop tables...");
			db.execSQL("DROP TABLE IF EXISTS " + User.TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + Status.TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + Draft.TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + Photo.TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + Comment.TABLE);

			// plugins...starting
			db.execSQL("DROP TABLE IF EXISTS " + Zhihu.TABLE);
			// plugins...ending

			// recreate...
			onCreate(db);
			Log.i(TAG, "finish upgrade table...");
		}
	}
}
