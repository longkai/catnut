/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import org.catnut.R;
import org.catnut.util.CatnutUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微博表情相关
 *
 * @author longkai
 */
public class TweetImageSpan implements Html.ImageGetter {

	private static final String TAG = "TweetImageSpan";
	/** 默认表情存放目录 */
	public static final String EMOTIONS_DIR = "emotions/";
	/** 微博表情匹配 [xx] */
	public static final Pattern EMOTION_PATTEN = Pattern.compile("\\[([^\\]\\[/ ]+)\\]");

	private Context mContext;

	// 为了保持有序，so换成linked的啦
	public static final Map<String, String> EMOTIONS = new LinkedHashMap<String, String>();

	static {
		EMOTIONS.put("[拜拜]", "baibai.png");
		EMOTIONS.put("[奥特曼]", "aoteman.png");
		EMOTIONS.put("[鄙视]", "bishi.png");
		EMOTIONS.put("[失望]", "shiwang.png");
		EMOTIONS.put("[闭嘴]", "bizui.png");
		EMOTIONS.put("[吃惊]", "chijing.png");
		EMOTIONS.put("[来]", "come_on.png");
		EMOTIONS.put("[酷]", "cool.png");
		EMOTIONS.put("[抓狂]", "zhuakuang.png");
		EMOTIONS.put("[衰]", "shuai.png");
		EMOTIONS.put("[馋嘴]", "chanzui.png");
		EMOTIONS.put("[晕]", "yun.png");
		EMOTIONS.put("[给力]", "geili.png");
		EMOTIONS.put("[浮云]", "fuyun.png");
		EMOTIONS.put("[good]", "good.png");
		EMOTIONS.put("[鼓掌]", "guzhang.png");
		EMOTIONS.put("[黑线]", "heixian.png");
		EMOTIONS.put("[哼]", "heng.png");
		EMOTIONS.put("[心]", "xin.png");
		EMOTIONS.put("[偷笑]", "touxiao.png");
		EMOTIONS.put("[神马]", "shenma.png");
		EMOTIONS.put("[花心]", "huaxin.png");
		EMOTIONS.put("[互粉]", "hufen.png");
		EMOTIONS.put("[囧]", "jiong.png");
		EMOTIONS.put("[打哈欠]", "kun.png");
		EMOTIONS.put("[挖鼻屎]", "wabishi.png");
		EMOTIONS.put("[可怜]", "kelian.png");
		EMOTIONS.put("[哈哈]", "haha.png");
		EMOTIONS.put("[蜡烛]", "lazhu.png");
		EMOTIONS.put("[懒得理你]", "landelini.png");
		EMOTIONS.put("[礼物]", "liwu.png");
		EMOTIONS.put("[爱你]", "aini.png");
		EMOTIONS.put("[马上有对象]", "duixiang.png");
		EMOTIONS.put("[太开心]", "taikaixin.png");
		EMOTIONS.put("[钱]", "money.png");
		EMOTIONS.put("[怒骂]", "numa.png");
		EMOTIONS.put("[不要]", "buyao.png");
		EMOTIONS.put("[ok]", "ok.png");
		EMOTIONS.put("[熊猫]", "xiongmao.png");
		EMOTIONS.put("[猪头]", "zhutou.png");
		EMOTIONS.put("[亲亲]", "qinqin.png");
		EMOTIONS.put("[兔子]", "tuzi.png");
		EMOTIONS.put("[弱]", "ruo.png");
		EMOTIONS.put("[泪]", "lei.png");
		EMOTIONS.put("[生病]", "shengbing.png");
		EMOTIONS.put("[害羞]", "haixiu.png");
		EMOTIONS.put("[思考]", "sikao.png");
		EMOTIONS.put("[睡觉]", "shuijiao.png");
		EMOTIONS.put("[呵呵]", "hehe.png");
		EMOTIONS.put("[汗]", "han.png");
		EMOTIONS.put("[吐]", "tu.png");
		EMOTIONS.put("[嘻嘻]", "xixi.png");
		EMOTIONS.put("[可爱]", "keai.png");
		EMOTIONS.put("[雪]", "xue.png");
		EMOTIONS.put("[伤心]", "shangxin.png");
		EMOTIONS.put("[威武]", "v5.png");
		EMOTIONS.put("[委屈]", "weiqu.png");
		EMOTIONS.put("[嘘]", "xu.png");
		EMOTIONS.put("[耶]", "ye.png");
		EMOTIONS.put("[左哼哼]", "zuohengheng.png");
		EMOTIONS.put("[右哼哼]", "youhengheng.png");
		EMOTIONS.put("[疑问]", "yiwen.png");
		EMOTIONS.put("[阴险]", "yinxian.png");
		EMOTIONS.put("[赞]", "zan.png");
		EMOTIONS.put("[挤眼]", "zuoguilian.png");
		EMOTIONS.put("[做鬼脸]", "zuoguilian.png"); // 可能有重复，我也搞不明白新浪怎么设置表情的...
		EMOTIONS.put("[玩去啦]", "lvxing.png");
		EMOTIONS.put("[悲伤]", "beishang.png");
		EMOTIONS.put("[怒]", "nu.png");
		EMOTIONS.put("[马到成功]", "madaochenggong.png");
		EMOTIONS.put("[书呆子]", "shudaizi.png");
		EMOTIONS.put("[话筒]", "huatong.png");
		EMOTIONS.put("[蛋糕]", "dangao.png");
		EMOTIONS.put("[感冒]", "ganmao.png");
		EMOTIONS.put("[男孩儿]", "nanhaier.png");
		EMOTIONS.put("[女孩儿]", "nvhaier.png");
		EMOTIONS.put("[顶]", "ding.png");
	}

	// is it worth?
	public static String[] EMOTION_KEYS = EMOTIONS.keySet().toArray(new String[EMOTIONS.size()]);

	private int mIconBound;

	public TweetImageSpan(Context context) {
		this.mContext = context;
		this.mIconBound = context.getResources().getInteger(R.integer.icon_bound_px);
	}

	public Spanned getImageSpan(CharSequence text) {
		Matcher m = EMOTION_PATTEN.matcher(text);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String key = m.group();
			if (EMOTIONS.containsKey(key)) {
				m.appendReplacement(sb, "<img src='" + EMOTIONS.get(key) + "' />");
			}
		}
		m.appendTail(sb);
		return Html.fromHtml(sb.toString(), this, null);
	}

	@Override
	public Drawable getDrawable(String source) {
		Drawable drawable = null;
		InputStream in = null;
		try {
			in = mContext.getAssets().open(EMOTIONS_DIR + source);
			drawable = Drawable.createFromStream(in, null);
		} catch (IOException e) {
			Log.wtf(TAG, "error open assets: " + source, e);
		} finally {
			CatnutUtils.closeIO(in);
		}
		if (drawable != null) {
			if (mIconBound == 0) {
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			} else {
				drawable.setBounds(0, 0, mIconBound, mIconBound);
			}
		}
		return drawable;
	}
}
