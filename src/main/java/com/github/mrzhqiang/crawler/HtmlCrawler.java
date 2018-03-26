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
import java.util.Scanner;
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
      System.out.println("文件:" + file + " 已存在，删除状态：" + file.delete());
      System.out.println("文件:" + file + " 重新生成状态：" + file.createNewFile());
    }

    Scanner scanner = new Scanner(System.in);
    System.out.println("请输入贴子的网址链接（输入 exit 退出）：");
    String line = scanner.nextLine();

    while (!line.equalsIgnoreCase("exit")) {
      try (BufferedWriter bw = new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")))) {
        // 首先找到当前页面的第一个贴子
        Element element = parseLink(line);
        System.out.println("连接成功，正在分析首页数据...");
        String pageCount = document.body().select("div[class=pg]").select("a[class=last]").text();
        System.out.println("发现共有：" + pageCount + " 个页面。");
        // 如果有贴子存在，那么页面有效
        while (element.childNodeSize() > 0) {
          // 记录所有贴子
          writeAllPostByDocument(bw, element);
          System.out.println("已完成第 " + postCount + " 页数据的分析...");
          // 找到下一页的链接
          String link =
              document.body().select("div[class=pg]").select("a[class=nxt]").attr("abs:href");
          // 下一页不存在，就结束循环
          if (link.equals("")) {
            System.out.println("下一页不存在，判定任务完成。");
            break;
          }
          System.out.println("找到下一页：" + link);
          // 访问链接，得到第一个贴子内容
          element = parseLink(link);
        }
        bw.flush();
      } catch (MalformedURLException e) {
        System.err.println("错误的网址：" + e.getMessage());
      } catch (IOException e) {
        System.err.println("访问失败：" + e.getMessage());
      }
      System.out.println();
      System.out.println("请输入贴子的网址链接（输入 exit 退出）：");
      line = scanner.nextLine();
    }
    System.out.println("程序结束！");
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
    // 最后一个【post】不属于发布内容
    while (element.childNodeSize() != 1) {
      // 楼层
      bw.append(element.select("strong").select("a").first().text());
      bw.append("\t");

      Elements authi = element.select("div[class=authi]");
      // 作者
      bw.append(authi.select("a[class=xi2]").text());
      bw.append("\t");
      // 时间
      bw.append(authi.select("em[id^=authorposton]").text());
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
