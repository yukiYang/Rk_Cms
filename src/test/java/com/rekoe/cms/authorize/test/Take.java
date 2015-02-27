package com.rekoe.cms.authorize.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.json.Json;
import org.nutz.lang.Files;
import org.nutz.lang.Lang;
import org.nutz.lang.Streams;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import com.rekoe.crawler.bean.CrawlerRuleBean;
import com.rekoe.crawler.bean.ExtendFieldsBean;
import com.rekoe.crawler.core.CrawlerController;
import com.rekoe.crawler.core.constants.CrawlerConfig;
import com.rekoe.crawler.core.data.CrawlScope;
import com.rekoe.crawler.core.filter.BriefAreaFilter;
import com.rekoe.crawler.core.filter.CommentAreaFilter;
import com.rekoe.crawler.core.filter.CommentFilter;
import com.rekoe.crawler.core.filter.CommentIndexFilter;
import com.rekoe.crawler.core.filter.CommentLinkFilter;
import com.rekoe.crawler.core.filter.ContentAreaFilter;
import com.rekoe.crawler.core.filter.FieldFilter;
import com.rekoe.crawler.core.filter.Filter;
import com.rekoe.crawler.core.filter.LinkAreaFilter;
import com.rekoe.crawler.core.filter.PaginationAreaFilter;
import com.rekoe.domain.CrawlerRule;
import com.rekoe.utils.CommonUtils;

public class Take {

	private final static Log log = Logs.get();
	private final static String PROPERTIES_CONF = "gather_core.properties";
	private PropertiesProxy conf = new PropertiesProxy();
	public static void main(String[] args) {
		Take take = new Take();
		take.init();
		Dao dao = IocProvider.ioc().get(Dao.class);
		CrawlerRule crawlerRule = dao.fetch(CrawlerRule.class,Cnd.where("id", "=", 1));
		take.s(crawlerRule);
	}
	
	public void init() {
		log.info("开始加载爬虫配置文件:" + PROPERTIES_CONF);
		conf.joinAndClose(Streams.fileInr(Files.findFile("D:\\Tools\\Gits\\Rk_Cms\\src\\test\\resources\\gathe_core.properties")));
		CrawlerConfig.threadNum = conf.getInt("threadNum", CrawlerConfig.threadNum);
		CrawlerConfig.taskTimeOut = conf.getInt("taskTimeOut", CrawlerConfig.taskTimeOut);
		CrawlerConfig.resSaveRootPath = conf.get("resSaveRootPath", CrawlerConfig.resSaveRootPath);
		CrawlerConfig.resSavePath = conf.get("resSavePath", CrawlerConfig.resSavePath);
		CrawlerConfig.extractResType = conf.get("extractResType", CrawlerConfig.extractResType);
		CrawlerConfig.extractMediaResType = conf.get("extractMediaResType", CrawlerConfig.extractMediaResType);
		CrawlerConfig.replaceResName = conf.get("replaceName", CrawlerConfig.replaceResName);
		//CrawlerConfig.proxyServerList = populateProxyServer(conf.get("proxyServerList"));
		CrawlerConfig.systemRootPath = conf.get("systemRootPath", CrawlerConfig.systemRootPath);
		CrawlerConfig.httpClientMaxConn = conf.getInt("httpClientMaxConn", CrawlerConfig.httpClientMaxConn);
		CrawlerConfig.httpClientMaxRoute = conf.getInt("httpClientMaxRoute", CrawlerConfig.httpClientMaxRoute);
		CrawlerConfig.httpConnTimeout = conf.getInt("httpConnTimeout", CrawlerConfig.httpConnTimeout);
		CrawlerConfig.httpSocketTimeout = conf.getInt("httpSocketTimeout", CrawlerConfig.httpSocketTimeout);
		CrawlerConfig.defaultWords = conf.get("defaultWords", CrawlerConfig.defaultWords);
		CrawlerConfig.defaultCommonReplaceWords = CommonUtils.populateWordsMap(conf.get("defaultCommonReplaceWords"));
		log.info("爬虫配置文件加载完成");

	}
	
	public void s(CrawlerRule rule)
	{
		CrawlerController crawlController = new CrawlerController();
		List<Filter<String, ?>> filters = new ArrayList<Filter<String, ?>>();
		filters.add(new LinkAreaFilter(rule.getLinksetStart(),rule.getLinksetEnd()));
		filters.add(new ContentAreaFilter(rule.getContentStart(),rule.getContentEnd()));
		filters.add(new BriefAreaFilter(rule.getDescriptionStart(),rule.getDescriptionEnd()));
		filters.add(new PaginationAreaFilter(rule.getPaginationStart(),rule.getPaginationEnd()));
		filters.add(new CommentIndexFilter(rule.getCommentIndexStart(),rule.getCommentIndexEnd()));
		filters.add(new CommentAreaFilter(rule.getCommentAreaStart(),rule.getCommentAreaEnd()));
		filters.add(new CommentFilter(rule.getCommentStart(),rule.getCommentEnd()));
		filters.add(new CommentLinkFilter(rule.getCommentLinkStart(),rule.getCommentLinkEnd()));
		
		//添加扩展字段过滤器
		if(StringUtils.isNotEmpty(rule.getKeywordsStart())){
			addFilter(rule.getKeywordsStart(),filters);
		}
		
		CrawlScope crawlScope = new CrawlScope();
		//crawlScope.setCrawlerPersistent(crawlerPersistent);
		crawlScope.setEncoding(rule.getPageEncoding());
		crawlScope.setId(rule.getId());
		crawlScope.setFilterList(filters);
		//评论内容列表是否与内容页分离，如果填写了,则为true
		if(StringUtils.isNotEmpty(rule.getCommentIndexStart())){
			crawlScope.setCommentListIsAlone(true);
		}
		crawlScope.setRepairPageUrl(rule.getLinkStart());
		crawlScope.setRepairImageUrl(rule.getLinkEnd());
		//设置休眠时间
		crawlScope.setSleepTime(rule.getPauseTime());
		crawlScope.setPaginationRepairUrl(rule.getPaginationRepairUrl());
		//是否下载图片至本地
		crawlScope.setExtractContentRes(rule.isExtractContentRes());
		//是否去掉内容中连接
		crawlScope.setReplaceHtmlLink(rule.isReplaceHtmlLink());
		crawlScope.setAllowRepeat(rule.isRepeatCheckType());
		crawlScope.setUseProxy(rule.isUseProxy());
		crawlScope.setProxyAddress(rule.getProxyAddress());
		crawlScope.setProxyPort(rule.getProxyPort());
		crawlScope.setReplaceWords(rule.getReplaceWords());
		crawlScope.addSeeds(rule.getAllPlans());
		crawlController.initialize(crawlScope);
		crawlController.start();
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addFilter(String jsonStr,List<Filter<String, ?>> filters){
		List<Map> arry = Json.fromJsonAsList(Map.class, jsonStr);
		String fields= "",filterStart= "",filterEnd = "";
		for(int i = 0;i < arry.size();i++){
			Map<String,String> map = arry.get(i);
			if(null != map.get("fields")){
				fields = map.get("fields");
			}
			if(null != map.get("filterStart")){
				filterStart =map.get("filterStart");
			}
            if(null != map.get("filterEnd")){
        	   filterEnd = map.get("filterEnd");
			}
			filters.add(new FieldFilter(fields,filterStart,filterEnd));
		}
	}
	private void startCrawker(CrawlerRuleBean rule){
		CrawlerController crawlController = new CrawlerController();
		List<Filter<String,?>> filters = new ArrayList<Filter<String,?>>();
		filters.add(new LinkAreaFilter(rule.getRuleContentBean().getLinksetStart(),rule.getRuleContentBean().getLinksetEnd()));
		filters.add(new ContentAreaFilter(rule.getRuleContentBean().getContentStart(),rule.getRuleContentBean().getContentEnd()));
		filters.add(new BriefAreaFilter(rule.getRuleContentBean().getDescriptionStart(),rule.getRuleContentBean().getDescriptionEnd()));
		//filters.add(new PaginationAreaFilter(rule.getRuleContentPageBean().getPaginationStart(),rule.getRuleContentPageBean().getPaginationEnd()));
		//filters.add(new CommentIndexFilter(rule.getRuleCommentBean().getCommentIndexStart(),rule.getRuleCommentBean().getCommentIndexEnd()));
		//filters.add(new CommentAreaFilter(rule.getRuleCommentBean().getCommentAreaStart(),rule.getRuleCommentBean().getCommentAreaEnd()));
		//filters.add(new CommentFilter(rule.getRuleCommentBean().getCommentStart(),rule.getRuleCommentBean().getCommentEnd()));
		//filters.add(new CommentLinkFilter(rule.getRuleCommentBean().getCommentLinkStart(),rule.getRuleCommentBean().getCommentLinkEnd()));
		
		
		List<Filter<String,Map<String,String>>> midFilters = new ArrayList<Filter<String,Map<String,String>>>();
		//添加过度连接过滤器
		if(null != rule.getRuleContentBean() && !Lang.isEmpty(rule.getRuleContentBean().getMidExtendFields())){
			addFilter(rule.getRuleContentBean().getMidExtendFields(),midFilters);
		}
		
		
		List<Filter<String,Map<String,String>>> multeityFilters = new ArrayList<Filter<String,Map<String,String>>>();
		//添加扩展字段过滤器
		if(null != rule.getRuleFieldsBean() && !Lang.isEmpty(rule.getRuleFieldsBean().getExtendFields())){
			addFilter(rule.getRuleFieldsBean().getExtendFields(),multeityFilters);
		}
		CrawlScope crawlScope = new CrawlScope();
		//crawlScope.setCrawlerPersistent(this.getCrawlerPersistent());
		crawlScope.setEncoding(rule.getRuleBaseBean().getPageEncoding());
		crawlScope.setId(rule.getRuleId());
		crawlScope.setFilterList(filters);
		crawlScope.setMidFilterList(midFilters);
		crawlScope.setMulteityFilterList(multeityFilters);
		//评论内容列表是否与内容页分离，如果填写了,则为true
		if(null != rule.getRuleCommentBean() && StringUtils.isNotEmpty(rule.getRuleCommentBean().getCommentIndexStart())){
			crawlScope.setCommentListIsAlone(true);
		}
		crawlScope.setRepairPageUrl(rule.getRuleBaseBean().getUrlRepairUrl());
		crawlScope.setRepairImageUrl(rule.getRuleBaseBean().getResourceRepairUrl());
		crawlScope.setPaginationRepairUrl(rule.getRuleBaseBean().getPaginationRepairUrl());
		//设置休眠时间
		crawlScope.setSleepTime(rule.getRuleBaseBean().getPauseTime());
		//是否下载图片至本地
		crawlScope.setExtractContentRes(Boolean.valueOf(rule.getRuleBaseBean().getSaveResourceFlag()));
		//是否去掉内容中连接
		crawlScope.setReplaceHtmlLink(Boolean.valueOf(rule.getRuleBaseBean().getReplaceLinkFlag()));
		crawlScope.setAllowRepeat(Boolean.valueOf(rule.getRuleBaseBean().getRepeatCheckFlag()));
		crawlScope.setUseProxy(Boolean.valueOf(rule.getRuleBaseBean().getUseProxyFlag()));
		crawlScope.setGatherOrder(Boolean.valueOf(rule.getRuleBaseBean().getGatherOrderFlag()));
		
		crawlScope.setProxyAddress(rule.getRuleBaseBean().getProxyAddress());
		crawlScope.setProxyPort(rule.getRuleBaseBean().getProxyPort());
		crawlScope.setReplaceWords(rule.getRuleBaseBean().getReplaceWords());

		//随机生成日期
		crawlScope.setDateFormat(rule.getRuleBaseBean().getDateFormat());
		crawlScope.setRandomDateFlag(Boolean.valueOf(rule.getRuleBaseBean().getRandomDateFlag()));
		crawlScope.setStartRandomDate(rule.getRuleBaseBean().getStartRandomDate());
		crawlScope.setEndRandomDate(rule.getRuleBaseBean().getEndRandomDate());
		crawlScope.setGatherNum(rule.getRuleBaseBean().getGatherNum());
		
		crawlScope.addSeeds(rule.getRuleContentBean().getAllPlans());
		
		crawlController.initialize(crawlScope);
		crawlController.start();
	}
	
	private void addFilter(List<ExtendFieldsBean> extendFields,List<Filter<String,Map<String,String>>> filters){
		for(ExtendFieldsBean extendFieldsBean : extendFields){
			filters.add(new FieldFilter(extendFieldsBean.getFields(),extendFieldsBean.getFilterStart(),extendFieldsBean.getFilterEnd()));
		}
	}
}
