# 【List、Collections、set】

##### 反馈和复习

```java
1.课下辅导讲题的速度太快了
2.上课重点内容多强调一下,非重点快速的过一遍
回顾:
	a.Collection集合体系
        Collection根接口
        	通用方法:
				add remove size isEmpty contains toArray clear
        	List 接口
        		ArrayList
        		LinkedList
        	Set 接口
        		HashSet
        		LinkedHahSet
        		TreeSet
    b.迭代器遍历
         Iterator<E> it = 集合对象.iterator();
		 while(it.hasNext()){
             E next =it.next();
             System.out.println(next)
         }
    c.泛型的定义和使用
        泛型类:
			public class 类名<E>{
                //在该类中就可以使用这个E类型
            }
        泛型方法:
			public <T> 返回值类型 方法名(数据类型 参数名,...){
                //在方法的参数上,方法体中就可以使用T这个类型
            }
        泛型接口:
			public class 接口名<E>{
                //在该接口中就可以使用这个E类型
            }
    d.常见数据结构    
        栈结构: 先进后出
        队列结构: 先进先出
        数组结构: 查询快,增删慢
        链表机构: 查询慢,增删快
        红黑树结构: 查询速度非常恐怖
            
```

##### 今日内容

```java
1.List接口以及他的实现类
2.Set接口以及他的实现类
3.Collections类(Arrays类)     
```

### 第一章 List接口

##### 1.List接口的特点

```java
a.有序的(Java中有序不是指123这种自然顺序,是指存入元素的顺序和打印集合时顺序是一致的) 
b.有索引
c.元素可以重复
```

##### 2.List接口中常用的方法以及常用子类

```java
List接口的常用方法(Collection根接口中继承的7个我们不再此处重复提起)
    增: add(int index,E e);
	删: remove(int index);
	改: set(int index,E e);
	查: get(int index);
List的常见实现类有哪些?
    ArrayList实现类
    LinkedList实现类
```

##### 3.ArrayList的数据结构以及使用

```java
ArrayList集合底层的数据结构: 数组结构(查询快,增删慢)
ArrayList集合他有哪些方法:
		7个(Collection接口中定义的)
        4个(List接口中定义的)
        没有特有方法   
            
使用一下ArrayList(省略)            
```

##### 4.LinkedList的数据结构以及使用

```java
LinkedList集合底层的数据结构: 链表结构(查询慢,增删快)
LinkedList集合他有哪些方法:
		7个(Collection接口中定义的)
    	4个(List接口中定义的)    
        8个特有方法(和首尾元素操作相关的方法)
       public void addFirst(E e);添加到开头
	   public void addLast(E e);添加到结尾
       public E removeFirst(); 删除首元素
	   public E removeLast(); 删除尾元素 
       public E getFirst();获取首元素
	   public E getLast(); 获取尾元素  
       public E pop(); 删除第一个元素,功能和removeFirst是一样的
	   public void push(E e); 添加到第一个元素,功能和addFirst的是一样的
            
public class LinkedListDemo {
    public static void main(String[] args) {
        //1.创建一个LinkedList
        LinkedList<String> arr = new LinkedList<String>();
        //2.添加
        arr.addLast("郭德纲");
        arr.addFirst("赵本山");
        arr.addLast("jack");
        arr.addFirst("rose");
        arr.addLast("刘德华");
        arr.addFirst("王宝强");
        //["王宝强" "rose" "赵本山" "郭德纲" "jack" "刘德华"]
        System.out.println(arr);
        System.out.println("---------------");
        //3.删除
        String removeFirst = arr.removeFirst();
        System.out.println(removeFirst);
        System.out.println(arr);
        System.out.println("---------------");
        String removeLast = arr.removeLast();
        System.out.println(removeLast);
        System.out.println(arr);
        System.out.println("---------------");

        //4.获取
        String first = arr.getFirst();
        System.out.println(first);
        System.out.println(arr);
        System.out.println("---------------");
        String last = arr.getLast();
        System.out.println(last);
        System.out.println(arr);
        System.out.println("---------------");
        //[rose, 赵本山, 郭德纲, jack]
        //5.pop push
        String pop = arr.pop();
        System.out.println("被删除的:"+pop);
        System.out.println(arr);
        System.out.println("---------------");
        //[赵本山, 郭德纲, jack]
        arr.push("tom");
        System.out.println(arr);
    }
}
            
```

##### 5 LinkedList源码分析[了解]

### 第二章 Collections类

##### 2.1 Collections的介绍

```java
Collections是一个集合工具类,其中方法都是静态的
```

##### 2.2 Collections常用功能

```java
public static <T> void sort(List<T> list);对集合的元素进行升序排序
public static void shuffle(List<?> list);对集合的元素进行随机打乱  

public class CollectionsDemo {
    public static void main(String[] args) {
        //1.创建一个集合
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(10);
        arr.add(44);
        arr.add(13);
        arr.add(54);
        arr.add(29);
        arr.add(22);
        arr.add(38);
        //2.排序
        System.out.println("排序之前:"+arr);
        Collections.sort(arr);
        System.out.println("排序之后:"+arr);
        //3.打乱顺序
        Collections.shuffle(arr);
        System.out.println("打乱之后:"+arr);
    }
}
```

##### 2.3 Comparator比较器排序

```java
public static <T> void sort(List<T> list,Comparator com);比较器排序

public class CollectionsDemo02 {
    public static void main(String[] args) {
        //1.创建一个集合
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(10);
        arr.add(44);
        arr.add(13);
        arr.add(54);
        arr.add(29);
        arr.add(22);
        arr.add(38);
        //2.比较器排序
        Collections.sort(arr, new Comparator<Integer>() {
            //compare称为比较方法
            //返回值表示比较的结果
            //返回值>0 说明 o1 > o2
            //返回值<0 说明 o1 < o2
            //返回值==0 说明 o1 == o2
            public int compare(Integer o1, Integer o2) {
                //口诀: 升序 前-后
                return o2 - o1;
            }
        });

        //3.打印
        System.out.println(arr);
    }
}

public class CollectionsDemo03 {
    public static void main(String[] args) {
        //1.创建一个集合
        ArrayList<Dog> dogs = new ArrayList<Dog>();
        //2.添加数据
        dogs.add(new Dog(10,"jack",4));
        dogs.add(new Dog(40,"tom",3));
        dogs.add(new Dog(30,"marry",2));
        dogs.add(new Dog(20,"hanmeimei",5));

        //3.对集合进行排序
        Collections.sort(dogs, new Comparator<Dog>() {
            @Override
            public int compare(Dog o1, Dog o2) {
                //口诀: 升序 前 - 后
                //需求: 按照狗的年龄降序
//                return o2.getAge() - o1.getAge();
                //需求: 按照狗的名字的长度升序
//                return o1.getName().length() - o2.getName().length();
                //需求: 按照狗的腿数和年龄的总和升序
//                return (o1.getLegs()+o1.getAge()) - (o2.getLegs()+o2.getAge());
                //需求: 按照狗的名字第一个字母 降序
                return o2.getName().charAt(0) - o1.getName().charAt(0);

            }
        });
        //4.打印
        for (Dog dog : dogs) {
            System.out.println(dog);
        }
    }
}
```

##### 2.4 可变参数

```java
格式:
	public void 方法名(数据类型... 参数名){
        
    }
作用:
	表达该方法参数个数可以任意(参数类型必须是指定的数据类型)
        
本质:
	可变本质其实是一个数组
        
 public class VariableArgumentDemo {
    public static void main(String[] args) {
        //调用方法
        System.out.println(getSum());
        System.out.println(getSum(1));
        System.out.println(getSum(1, 2));
        System.out.println(getSum(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }
    //定义方法,求多个整数的和
    public static int getSum(int... num){
        System.out.println(num);
        //求和
        int sum = 0;
        for (int i : num) {
            sum+=i;
        }
        return sum;
    }
}    

限制:
	a.一个方法中只能有一个可变参数
    b.一个方法中既有可变参数,又有普通参数,这是允许,但是可变参数必须写到最后     
```

##### 2.5 可变参数的应用场景

```java
public class VariableArgumentDemo02 {
    public static void main(String[] args) {
        //1.创建集合
        ArrayList<String> arr = new ArrayList<String>();
        //2.添加5个元素
//        arr.add("java1","java2","java3","java4","java5");报错!!!
        //3.使用Collections工具类
        Collections.addAll(arr,"java1","java2","java3","java4","java5");
        System.out.println(arr);
    }
}
```

### 第三章 Set接口

##### 3.1 Set接口的特点

```java
a.无序的(无序在Java是指存入时元素的顺序和打印集合时元素的顺序不能保证一致)(LinkedHashSet除外)
b.无索引
c.元素唯一的
注意:
	Set接口有一个实现类不是无序的,是有序的,LinkedHashSet
        
```

##### 3.2 Set接口的常用方法以及常用子类

```java
Set接口常用方法:
	7个Collection中定义的方法
    没有特有方法
Set接口的实现类:
	HashSet:无序的,无索引,元素唯一的  3 4 1 ==> 3 1 4
    LinkedHashSet:无索引,元素唯一的,但是它是有序的 3 41  ==> 341
    TreeSet:无序的(但是有自然顺序的),无索引,元素唯一的   4 3 1 ===> 1 3 4 
```

##### 3.3 HashSet的数据结构以及使用

```java
HashSet的底层数据结构: 哈希表结构(数组+链表+红黑树<JDK8中新增>)
HashSet的常见方法:
	7个Collection中定义的方法
    0个Set中定义的方法
    0个特有方法
HashSet的特定:
	无序的,无索引,元素唯一的
        
public class HashSetDemo {
    public static void main(String[] args) {
        //1.创建一个HashSet
        HashSet<String> set = new HashSet<String>();
        //2.添加
        set.add("java");
        set.add("php");
        set.add("c++");
        set.add("python");
        System.out.println(set);//[c++, python, java, php] 说明是无序的
        //没有带索引的方法,说明是无索引的
        set.add("python");
        set.add("python");
        set.add("python");
        set.add("python");
        set.add("python");
        System.out.println(set); //集合只有一个python,说明元素是唯一的
    }
}        
```

##### 3.4 LinkedHashSet的数据结构以及使用

```java
LinkedHashSet的底层数据结构: 链表+哈希表结构
LinkedHashSet的常见方法:
		7个Collection中定义的方法
    	0个Set中定义的方法
    	0个特有方法
            
LinkedHashSet的特点:
	有序的!!!!!! 无索引,元素唯一的
        
public class LinkedHashSetDemo {
    public static void main(String[] args) {
        //1.创建一个LinkedHashSet对象
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        //2.添加
        set.add("php");
        set.add("python");
        set.add("java");
        set.add("c++");
        System.out.println(set);//[php, python, java, c++] 说明他是有序的
        //没有带索引的方法,说明他是无索引的
        set.add("java");
        set.add("c++");
        set.add("python");
        set.add("php");
        System.out.println(set); //[php, python, java, c++] 说明他也是元素唯一的
    }
}
        
```

##### 3.5 TreeSet的数据结构以及使用

```java
TreeSet的底层数据结构: 红黑树结构(查询效率的非常恐怖的)
TreeSet的常见方法:
		7个Collection中定义的方法
    	0个Set中定义的方法
    	0个特有方法    
TreeSet的特点:
		无序的(默认升序的),无索引,元素唯一的
            
public class TreeSetDemo {
    public static void main(String[] args) {
        //1.创建TreeSet对象
        TreeSet<Integer> set = new TreeSet<Integer>();
        //2.添加
        set.add(5);
        set.add(10);
        set.add(1);
        set.add(8);
        set.add(9);
        System.out.println(set);//[1, 5, 8, 9, 10] 说明他是无序的,但是有自然顺序(默认升序的)
        //没有带索引的方法,说明他是无索引的
        set.add(8);
        set.add(8);
        set.add(8);
        System.out.println(set);//[1, 5, 8, 9, 10] 说明他的元素也是唯一的
    }
}            
```

##### 3.6 哈希表结构的介绍[扩展]

- 对象的哈希值(对象的"数字指纹")

  ```java
  a.如何查看对象的哈希值呢??
      非常简单,调用对象的hashCode方法,即可得到该对象的哈希值
  b.其实我们以前说的地址值都是假的,它其实是哈希值的16进制而已
      public String toString() {
          return getClass().getName() + "@" + Integer.toHexString(hashCode());
      	//从以上代码就可以看出,我们看到地址值,其实是根据获取到hash值转成后的16进制表示
      }
  	public native int hashCode(); 哈希值的计算不是Java写的,是c/c++写的,
  								底层通过哈希算法计算出的,所有这个值哈希值
                                      
  c.在Java中有没有真正的地址值呢??
     有! 对象名中保存的其实就是真正的地址值
     例如: Dog d1 = new Dog(2,"jack"); d1中保存的其实就是真正的地址值
     但是无法打印出来:
  		System.out.println(d1); ==> System.out.println(d1.toString());
  
  d.hashCode方法我们能否重写??
      必须可以,重写后不管hashCode和还是toString都会调用重写后的hashCode方法                         
  ```
  
  ==结论:哈希表结构如何保证元素的唯一性?==
  
  ```java
  哈希值结构怎么判断两个元素是否重复???
      非常简单: 先比较两个对象的哈希值,如果哈希值一样,再调用equals方法,
  			只有哈希值一样且equals方法返回值是true,才判定这两个元素重复
  ```

##### ==3.7 哈希表结构保存自定义类型练习==

```java
使用哈希表结构的集合保存自定义对象时,为了保证集合中对象的唯一性,我们必须重写hashCode和equals方法
public class Cat {
    private int age;
    private String name;

    public Cat() {
    }

    public Cat(int age, String name) {
        this.age = age;
        this.name = name;
    }

    //重写hashCode和equals,使其只跟内容有关
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cat cat = (Cat) o;
        return age == cat.age &&
                Objects.equals(name, cat.name);
    }
    @Override
    public int hashCode() {
        return Objects.hash(age, name);
    }
    @Override
    public String toString() {
        return "Cat{" +
                "age=" + age +
                ", name='" + name + '\'' +
                '}';
    }
}
public class TestHashDemo02 {
    public static void main(String[] args) {
        //1.创建一个具有哈希值结构的集合
        HashSet<Cat> cats = new HashSet<Cat>();
        //2.添加
        cats.add(new Cat(1,"jack"));
        cats.add(new Cat(2,"tom"));
        cats.add(new Cat(3,"marry"));
        cats.add(new Cat(4,"jerry"));
        cats.add(new Cat(4,"jerry"));
        //如果想要内容一样的对象,hashCode和equals相同,那么重写hashCode和equals,让这两个方法只能内容有关即可
        //3.打印
        for (Cat cat : cats) {
            System.out.println(cat);
        }
    }
}    
```

##### 总结

```java
1.Collection集合体系
    根接口:Collection(7个方法)
        子接口:List(4个,有序,有索引,可重复)
            实现类:ArrayList(0个)
            实现类:LinkedList(8个)    
        子接口:Set(0个,无序,无索引,不可重复)
            实现类:HashSet(0个,无序,无索引,不可重复)
            实现类:LinkedHashSet(0个,有序,无索引,不可重复)  
    		实现类:TreeSet(0个,无序(自然顺序),无索引,不可重复)
2.Collections工具类 
        shuffle sort
        public static void sort(集合,new Comparator<集合的泛型>(){
            	public int compare(对象1,对象2){
                 	//升序 1-2   
                }
        });       
3.哈希表通过哈希值和equals双重标准,判断元素是否重复
    所有当我们使用哈希值结构的集合来保存自定义类型时,需要在自定义类中重写hashCOde和equals方法,以保证哈希表的唯一性!!
```



