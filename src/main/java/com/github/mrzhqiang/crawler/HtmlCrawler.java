package com.github.mrzhqiang.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class HtmlCrawler {
  private static final boolean isSkipFirstPost = true;
  private static int postCount = 0;
  private static Document document;

  private static final File file = new File("贴子.txt");

  public static void main(String[] args) throws IOException {
    if (file.exists()) {
      System.out.println("File:" + file + " delete is " + file.delete());
      System.out.println("File:" + file + " created is " + file.createNewFile());
    }
    BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
    try {
      // 首先找到当前页面的第一个贴子
      Element element = parseLink("http://club.huawei.com/thread-15137624-1-1.html");
      // 如果有贴子存在，那么页面有效
      while (element.childNodeSize() > 0) {
        // 记录所有贴子
        writeAllPostByDocument(bw, element);
        // 找到下一页的链接
        String link =
            document.body().select("div[class=pg]").select("a[class=nxt]").attr("abs:href");
        // 下一页不存在，就结束循环
        if (link.equals("")) {
          break;
        }
        // 访问链接，得到第一个贴子内容
        element = parseLink(link);
      }
      bw.flush();
    } catch (MalformedURLException e) {
      System.err.println("错误的网址：" + e.getMessage() + ",请重新输入");
    } catch (IOException e) {
      System.err.println("访问失败：" + e.getMessage() + ",请重新尝试");
    } finally {
      bw.close();
    }
  }

  private static Element parseLink(String link) throws IOException {
    // http://club.huawei.com/thread-15137624-1-1.html
    // http://club.huawei.com/thread-15137624-2451-1.html
    URL url = new URL(link);
    document = Jsoup.parse(url, (int) TimeUnit.SECONDS.toMillis(10));
    return document.body()
        .select("div[id=postlist]")
        .select("div[id^=post_]")
        .first();
  }

  private static void writeAllPostByDocument(BufferedWriter bw, Element element)
      throws IOException {
    // 如果是第一页，那么就是主题帖，需要跳过它
    if (postCount == 0 && isSkipFirstPost) {
      element = element.nextElementSibling();
    }
    // 最后一个贴子不属于发布内容
    while (element.childNodeSize() != 1) {
      // 楼层
      bw.append(element.select("strong").select("a").first().text());
      bw.append("\t");
      // 作者
      bw.append(element.select("div[class=authi]").select("a[class=xi2]").text());
      bw.append("\t");
      // 内容
      Elements elements = element.select("td[class=t_f]");
      String content =
          findPostContent(elements.first().childNodes()) + findPostImage(
              elements.select("img"));
      bw.append(content);
      bw.newLine();
      element = element.nextElementSibling();
    }
    // 页面统计
    postCount++;
  }

  private static String findPostContent(List<Node> nodes) {
    StringBuilder builder = new StringBuilder();
    for (Node node : nodes) {
      switch (node.nodeName()) {
        case "#text":
          builder.append(node.toString());
          break;
        default:
          break;
      }
    }
    return builder.toString();
  }

  private static String findPostImage(Elements select) {
    StringBuilder builder = new StringBuilder(select.size());
    for (Element aSelect : select) {
      builder.append("\t").append(aSelect.attr("file"));
    }
    return builder.toString();
  }
}
