/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.support;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by longkai on 14-2-10.
 */
public class TweetImageSpan implements Html.ImageGetter {

	private static final String TAG = "TweetImageSpan";
	/** 微博表情匹配 [xx] */
	public static final Pattern EMOTION_PATTEN = Pattern.compile("\\[([^\\]\\[/ ]+)\\]");

	private Context mContext;

	private static final Map<String, String> EMOTIONS = new HashMap<String, String>();

	static {
		EMOTIONS.put("[草泥马]","shenshou_org");
		EMOTIONS.put("[神马]","horse2_org");
		EMOTIONS.put("[浮云]","fuyun_org");
		EMOTIONS.put("[给力]","geili_org");
		EMOTIONS.put("[围观]","wg_org");
		EMOTIONS.put("[威武]","vw_org");
		EMOTIONS.put("[熊猫]","panda_org");
		EMOTIONS.put("[兔子]","rabbit_org");
		EMOTIONS.put("[奥特曼]","otm_org");
		EMOTIONS.put("[囧]","j_org");
		EMOTIONS.put("[互粉]","hufen_org");
		EMOTIONS.put("[礼物]","liwu_org");
		EMOTIONS.put("[呵呵]","smilea_org");
		EMOTIONS.put("[嘻嘻]","tootha_org");
		EMOTIONS.put("[哈哈]","laugh");
		EMOTIONS.put("[可爱]","tza_org");
		EMOTIONS.put("[可怜]","kl_org");
		EMOTIONS.put("[挖鼻屎]","kbsa_org");
		EMOTIONS.put("[吃惊]","cj_org");
		EMOTIONS.put("[害羞]","shamea_org");
		EMOTIONS.put("[挤眼]","zy_org");
		EMOTIONS.put("[闭嘴]","bz_org");
		EMOTIONS.put("[鄙视]","bs2_org");
		EMOTIONS.put("[爱你]","lovea_org");
		EMOTIONS.put("[泪]","sada_org");
		EMOTIONS.put("[偷笑]","heia_org");
		EMOTIONS.put("[亲亲]","qq_org");
		EMOTIONS.put("[生病]","sb_org");
		EMOTIONS.put("[太开心]","mb_org");
		EMOTIONS.put("[懒得理你]","ldln_org");
		EMOTIONS.put("[右哼哼]","yhh_org");
		EMOTIONS.put("[左哼哼]","zhh_org");
		EMOTIONS.put("[嘘]","x_org");
		EMOTIONS.put("[衰]","cry");
		EMOTIONS.put("[委屈]","wq_org");
		EMOTIONS.put("[吐]","t_org");
		EMOTIONS.put("[打哈欠]","k_org");
		EMOTIONS.put("[抱抱]","bba_org");
		EMOTIONS.put("[怒]","angrya_org");
		EMOTIONS.put("[疑问]","yw_org");
		EMOTIONS.put("[馋嘴]","cza_org");
		EMOTIONS.put("[拜拜]","bye_org");
		EMOTIONS.put("[思考]","sk_org");
		EMOTIONS.put("[汗]","sweata_org");
		EMOTIONS.put("[困]","sleepya_org");
		EMOTIONS.put("[睡觉]","sleepa_org");
		EMOTIONS.put("[钱]","money_org");
		EMOTIONS.put("[失望]","sw_org");
		EMOTIONS.put("[酷]","cool_org");
		EMOTIONS.put("[花心]","hsa_org");
		EMOTIONS.put("[哼]","hatea_org");
		EMOTIONS.put("[鼓掌]","gza_org");
		EMOTIONS.put("[晕]","dizzya_org");
		EMOTIONS.put("[悲伤]","bs_org");
		EMOTIONS.put("[抓狂]","crazya_org");
		EMOTIONS.put("[黑线]","h_org");
		EMOTIONS.put("[阴险]","yx_org");
		EMOTIONS.put("[怒骂]","nm_org");
		EMOTIONS.put("[心]","hearta_org");
		EMOTIONS.put("[伤心]","unheart");
		EMOTIONS.put("[猪头]","pig");
		EMOTIONS.put("[ok]","ok_org");
		EMOTIONS.put("[耶]","ye_org");
		EMOTIONS.put("[good]","good_org");
		EMOTIONS.put("[不要]","no_org");
		EMOTIONS.put("[赞]","z2_org");
		EMOTIONS.put("[来]","come_org");
		EMOTIONS.put("[弱]","sad_org");
		EMOTIONS.put("[蜡烛]","lazu_org");
		EMOTIONS.put("[钟]","clock_org");
		EMOTIONS.put("[话筒]","m_org");
		EMOTIONS.put("[蛋糕]","cake");
		EMOTIONS.put("[马上有对象]","mashangyouduixiang_org");
		EMOTIONS.put("[马上拜年]","mashangbainian_org");
		EMOTIONS.put("[让红包飞]","hongbaofei2014_org");
		EMOTIONS.put("[求红包]","lxhhongbao2014_org");
		EMOTIONS.put("[青啤鸿运当头]","hongyun_org");
		EMOTIONS.put("[xkl恭喜]","xklgongxi_org");
		EMOTIONS.put("[xkl发财]","xklfacai_org");
		EMOTIONS.put("[过年啦]","lxhguonianla_org");
		EMOTIONS.put("[福到啦]","lxhfudaola_org");
		EMOTIONS.put("[大红灯笼]","lxhdahongdenglong_org");
		EMOTIONS.put("[雪]","snow_org");
		EMOTIONS.put("[bobo拜年]","bobolongnian_org");
		EMOTIONS.put("[mtjj拜年]","longnianmtjj_org");
		EMOTIONS.put("[mk拜年]","longnianmk_org");
		EMOTIONS.put("[gst回家啦]","gsthuijiala_org");
		EMOTIONS.put("[dada提灯笼]","dadadenglong_org");
		EMOTIONS.put("[鸿运当头]","hongyun_org");
		EMOTIONS.put("[lt火车票]","lttickets_org");
		EMOTIONS.put("[lt新年好]","ltxinnianhao_org");
		EMOTIONS.put("[din癫当xmas]","dindongxmas_org");
		EMOTIONS.put("[平安果]","lxh_apple_org");
		EMOTIONS.put("[圣诞树]","christree_org");
		EMOTIONS.put("[圣诞袜]","chrisocks_org");
		EMOTIONS.put("[圣诞帽]","chrishat_org");
		EMOTIONS.put("[Lavida生活]","lavida_org");
		EMOTIONS.put("[ali做鬼脸]","alizuoguiliannew_org");
		EMOTIONS.put("[光棍节]","lxh1111_org");
		EMOTIONS.put("[劲能样]","jingnengyang_org");
		EMOTIONS.put("[微博益起来]","yiqilai_org");
		EMOTIONS.put("[微公益爱心]","lxhgongyi_org");
		EMOTIONS.put("[放假啦]","lxhfangjiale_org");
		EMOTIONS.put("[国旗]","flag_org");
		EMOTIONS.put("[带着微博去旅行]","weitripballoon_org");
		EMOTIONS.put("[玩去啦]","weitrip_org");
		EMOTIONS.put("[ali哇]","aliwanew_org");
		EMOTIONS.put("[xkl转圈]","xklzhuanquan_org");
		EMOTIONS.put("[酷库熊顽皮]","kxwanpi_org");
		EMOTIONS.put("[bm可爱]","bmkeai_org");
		EMOTIONS.put("[BOBO爱你]","boaini_org");
		EMOTIONS.put("[转发]","lxhzhuanfa_org");
		EMOTIONS.put("[偷乐]","lxhtouxiao_org");
		EMOTIONS.put("[得意地笑]","lxhdeyidixiao_org");
		EMOTIONS.put("[泪流满面]","lxhtongku_org");
		EMOTIONS.put("[ppb鼓掌]","ppbguzhang_org");
		EMOTIONS.put("[din推撞]","dintuizhuang_org");
		EMOTIONS.put("[xb压力]","xbyali_org");
		EMOTIONS.put("[moc转发]","moczhuanfa_org");
		EMOTIONS.put("[lt切克闹]","ltqiekenao_org");
		EMOTIONS.put("[江南style]","gangnamstyle_org");
		EMOTIONS.put("[笑哈哈]","lxhwahaha_org");
	}

	public TweetImageSpan(Context mContext) {
		this.mContext = mContext;
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
		String src = mContext.getPackageName() + ":raw/" + source;
		int resId = mContext.getResources().getIdentifier(src, null, null);
		Drawable drawable = null;
		if (resId != 0) {
			drawable = mContext.getResources().getDrawable(resId);
		}
		if (drawable != null) {
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		}
		return drawable;
	}
}
