package mario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.JComponent;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//********************* element planszy
//klasa bazowa dla segmentow
interface IPolaczenie {
    int get(int indeks);
    void set(int indeks, int c);
    int geti();
    int length();
}
class Baza {
    
    private int[] tab = new int[100];
    private Baza(){}
    private static Baza baza;
    
    private static Baza getBaza(){ 
        if(baza==null) baza=new Baza();
        return baza;
    }
    
    public static IPolaczenie getPolaczenie() {
        return Polaczenie.getInstance();
    }
    
    private static class Polaczenie implements IPolaczenie {
        private Baza baza= Baza.getBaza();
        private static Polaczenie[] polaczenia ={ new Polaczenie(),new Polaczenie() ,new Polaczenie()};
        private static int index=-1;
        private Polaczenie(){}
        
        public static IPolaczenie getInstance() {
            index++;
            return polaczenia[index];
        }
        
        public int get(int index)
        {
            return baza.tab[index];
        }
        public void set(int index, int c) {
            baza.tab[index]=c;
        }
        public int length() {
            return baza.tab.length;
        }
        public int geti()
        {
            return this.index;
        }
    }
}
class Segment {	
	protected Image img;
	protected int x, y;
	protected int W, H;
	public Segment(int x, int y, String file) {
		this.x = x;
		this.y = y;
		img = new ImageIcon(file).getImage();
		W = img.getWidth(null);
		H = img.getHeight(null);
	}
	public Rectangle getBounds()	{
		return new Rectangle(x, y, W, H);
	}
	public void draw(Graphics g) {
		g.drawImage(img, x, y, null);
	}
	public void tick() {}
	public void collisionV(Sprite sprite)	{}
	public void collisionH(Sprite sprite)	{}
}
//segment bez mozliwosci przejscia
class SegmentBlock extends Segment {	
	public SegmentBlock(int x, int y, String file)	{
		super(x,y,file);
	}
	public void collisionV(Sprite sprite)	{
			sprite.stopJump();
	}
	public void collisionH(Sprite sprite)	{
			sprite.stopMove();
	}
        
}
//segment, na ktory mozna wskoczyc 
class SegmentBlockV extends Segment {
	public SegmentBlockV(int x, int y, String file)	{
		super(x,y,file);
	}
	public void collisionV(Sprite sprite)	{
			if(sprite.jumpingDown() && sprite.getBottomY()==y)
				sprite.stopJump();
	}
}
class SegmentBlockF extends Segment {
	private int[] anim;	
        
	public SegmentBlockF(int x, int y, String file, int[] sequence) {
		super(x,y,file);
		anim = sequence; 
	}
        public int frame = 0;
	public void tick()	{
		frame++;
		while (frame >= anim.length)
			frame -= anim.length;
	}
		
	public void draw(Graphics g)	{
		g.drawImage(img,x,y,x + W,y + H/4,
			0,anim[frame]*H/4,W,anim[frame]*H/4 + H/4,null);
	}
	public void collisionV(Sprite sprite)	{
                            sprite.nonalive();

	}
        public void collisionH(Sprite sprite)	{
			
            if(sprite.getY()!=y)
                            sprite.nonalive();
                        
	}
        public Rectangle getBounds()	{
		return new Rectangle(x, y, W, H/4);
	}        
}
class SegmentE extends Segment {
        public SegmentE(int x, int y, String file)	{
		super(x,y,file);
	}
        public void collisionH(Sprite sprite)	{
                      System.exit(0);      
                        
	}
        public void collisionV(Sprite sprite)	{
                      System.exit(0);                      
        }
    }

//segment animowany
class SegmentAnim extends Segment {
	private int[] anim;	
	public SegmentAnim(int x, int y, String file, int[] sequence) {
		super(x,y,file);
		anim = sequence; 
	}
	public int frame = 0;
	public void tick()	{
		frame++;
		while (frame >= anim.length)
			frame -= anim.length;
	}
		
	public void draw(Graphics g)	{
		g.drawImage(img,x,y,x + W,y + H/4,
			0,anim[frame]*H/4,W,anim[frame]*H/4 + H/4,null);
	}
        public void collisionV(Sprite sprite)	{

                            sprite.coin();
                            x=1010;
                            y=1010;

        }
}

//************************* postac gracza

class Sprite {
	private static final Image img = new ImageIcon("Mario.png").getImage();
	public boolean alive =true;
	private int[] anim = {0, 1, 2, 1};
	private int frame = 2;		// klatka animacji
	private boolean mirror = false; // postac patrzy w lewo/ prawo
	private int moving = 0;		// ruch w poziomie
	private int jumping = 0; 	// ruch w pionie
	private final ArrayList<Segment> plansza;
	private int x=150, y=100; 	// pozycja na ekranie
	private final int W=16, H=27;// wysokosc i szerokosc sprite'a
        IPolaczenie p1 ;
	public Sprite(ArrayList<Segment> pl) { plansza=pl;
        p1=Baza.getPolaczenie();
        }
        //private Punkty pkt;
        private int points = 0;
	public int getX() { return x; }
	public int getY() { return y; }
	public int getBottomY() { return y+H; }
        public int getLeftX(){return x-W;}
        public int getRightX(){return x+W;}
        
	public void jump() {		// poruszanie postacia
		if(jumping == 0) jumping = 10;
	}
	public boolean isJumping() { return jumping>0; }
	public boolean jumpingDown() { return jumping<0; }
	public void stopJump() { jumping=0; }
	public void stopMove() { moving=0; }

	public void left() {
		moving = -5;
		mirror = false;
	}
	public void right() {
		moving = 5;
		mirror = true;
	}
	public void stop() {
		moving = 0;
		frame = 2;
	}
		
	private boolean canGo(int dx, int dy)	{
		for(Segment s:plansza)
			if(s.getBounds().intersects(x+dx, y+dy, W, H))	{
				return false;
			}
		return true;
	}
	private void collide(int dx, int dy)	{
		for(Segment s:plansza)
			if(s.getBounds().intersects(x+dx, y+dy, W, H))	{
				if(dx != 0)
					s.collisionH(this);
				if(dy != 0)
					s.collisionV(this);
			}
	}
	public void tick() {
		if(moving != 0) {// animacja ruchu
			frame++;
			while (frame >= anim.length)
				frame -= anim.length;
		}
		// przesuniecie w poziomie
		for(int i = 0; i < Math.abs(moving); ++i) {
			collide((int)Math.signum(moving), 0);
			x += (int)Math.signum(moving);
		}
			
		// przesuniecie w pionie
		for(int i = 0; i < Math.abs(jumping); ++i) {
			collide(0, -(int)Math.signum(jumping));
			y -= (int)Math.signum(jumping);
		}
		// czy mamy grunt pod nogami?
		jumping--;
		collide(0,1);
		if(jumping != 0)	frame = 0;
		if(jumping == 0 && moving == 0)	frame = 2;
	}
	public void draw(Graphics g) {
		g.drawImage(img, x + (mirror?W:0),y,x + (mirror?0:W),y + H,
			anim[frame]*W,0,anim[frame]*W + W,H,null);
	}
        public void nonalive()
        {
            
            try {
                this.alive=false;
                System.out.println("Gameover!");
                JFrame f= new JFrame("Frame");
                Szablonowa7 m = new Szablonowa7();
                f.getContentPane().add(m);
                f.setSize(500, 500);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
                f.pack();
                new Thread(m).start();
                JOptionPane.showMessageDialog(null, "Twoj wynik to: "+p1.get(0));
                TimeUnit.SECONDS.sleep(250);
                f.setVisible(false);
                
            } catch (InterruptedException ex) {
                Logger.getLogger(Sprite.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        public void coin()
        {
            p1.set(p1.geti(),p1.get(p1.geti())+50);
            System.out.println(p1.get(0));
        }
}
class SpriteController implements Runnable {
	private final Sprite sprite;
	private final ArrayList<Segment> plansza;
	private final JPanel panel;
	public SpriteController(Sprite sp, ArrayList<Segment> pl, JPanel pan) {
		sprite=sp;
		plansza=pl;
		panel=pan;
	}
	public void run() {
		while(true) {
			sprite.tick();
			for(Segment s:plansza)
				s.tick();
			panel.repaint();
			Thread.currentThread().yield();
			try {
				Thread.currentThread().sleep(40);
			}catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}
	

class Game extends JPanel {
	private final int TILESIZE = 32;
	private ArrayList<Segment> plansza;
	private Sprite sprite;
	private ArrayList<Segment> stworzPlansze(String plik, Budowniczy budowniczy) {
		BufferedReader br=null;
		try {
			br=new BufferedReader(new FileReader(plik));
			ArrayList<Segment> plansza=new ArrayList<Segment>();
			String linia;
			int x, y=4, liczba, znaki;
			char znak, cyfra1, cyfra2;
			while ((linia=br.readLine())!=null)  {
				x=4;znaki=0;
				while ((linia.length()-znaki)>=3) { 
					znak=linia.charAt(znaki++);
					cyfra1=linia.charAt(znaki++);
					cyfra2=linia.charAt(znaki++);
					liczba=(cyfra1-'0')*10+(cyfra2-'0');
					switch (znak) {
						case 'X':
							x+=liczba*TILESIZE;
							break;
						case 'A':
							for (int i=0;i<liczba;++i) {
								budowniczy.dodajSegmentA(x, y);
								x+=TILESIZE;
							}
							break;
						case 'B':
							for (int i=0;i<liczba;++i) {
								budowniczy.dodajSegmentB(x, y);
								x+=TILESIZE;
							}
							break;
						case 'C':
							for (int i=0;i<liczba;++i) {
								budowniczy.dodajSegmentC(x, y);
								x+=TILESIZE;
							}
							break;
						case 'G':
							for (int i=0;i<liczba;++i) {
								budowniczy.dodajSegmentG(x, y);
								x+=TILESIZE;
							}
							break;
                                                case 'F':
                                                        for(int i=0;i<liczba;++i)
                                                        {
                                                            budowniczy.dodajSegmentF(x, y);
                                                            x+=TILESIZE;
                                                        }
                                                        break;
                                                case 'P':
                                                        for(int i=0;i<liczba;++i)
                                                        {
                                                            budowniczy.dodajSegmentCloud(x, y);
                                                            x+=TILESIZE;
                                                        }
                                                        break;
                                                case 'E':
                                                        for(int i=0; i<liczba;++i)
                                                        {
                                                            budowniczy.dodajSegmentE(x,y);
                                                            x+=TILESIZE;
                                                        }
                                                        break;
                                                case 'S':
                                                        for(int i=0; i<liczba;++i)
                                                        {
                                                            budowniczy.dodajSegmentEmpty(x,y);
                                                            x+=TILESIZE;
                                                        }
                                                        
                                                
                                                        
					}
				}
				y+=TILESIZE;
			}
			br.close();
			return budowniczy.pobierzPlansze();
		} catch (IOException e) { 
			System.out.println("Blad wczytania planszy");
			e.printStackTrace();
			return null;
		}
	}
	public Game(String plik) {
		setPreferredSize(new Dimension(1000, 800));
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent ev) {
				switch(ev.getKeyCode())	{
					case KeyEvent.VK_LEFT:
					case KeyEvent.VK_RIGHT:
						sprite.stop();
						break;
				}
			}
			public void keyPressed(KeyEvent ev) {
				switch(ev.getKeyCode())	{
					case KeyEvent.VK_LEFT:	sprite.left(); break;
					case KeyEvent.VK_RIGHT:	sprite.right(); break;
					
					case KeyEvent.VK_SPACE:
					case KeyEvent.VK_UP:
						sprite.jump(); break;
				}
			}
		});
			
		setFocusable(true);
                Budowniczy budowniczy = new BudowniczyZwykly();
		plansza=stworzPlansze(plik, budowniczy);
		sprite=new Sprite(plansza);
		new Thread(new SpriteController(sprite, plansza, this)).start();
	}
	public void paint(Graphics g)	{
		super.paint(g);
		for(Segment s:plansza)
			s.draw(g);
		sprite.draw(g);
	}

}

interface Budowniczy {
    static ArrayList<Segment> tablicaSegmentow = new ArrayList();
    void dodajSegmentA(int x, int y);
    void dodajSegmentB(int x, int y);
    void dodajSegmentC(int x, int y);
    void dodajSegmentG(int x, int y);
    void dodajSegmentF(int x, int y);
    void dodajSegmentGO(int x, int y);
    void dodajSegmentCloud(int x, int y);
    void dodajSegmentEmpty(int x,int y);
    void dodajSegmentE(int x,int y);
    ArrayList<Segment> pobierzPlansze();
}

class BudowniczyZwykly implements Budowniczy {
    
    public void dodajSegmentA(int x, int y) {
        Segment s=new SegmentBlock(x, y, "block1.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentB(int x, int y) {
        Segment s=new SegmentBlockV(x, y, "block2.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentC(int x, int y) {
        Segment s=new Segment(x, y, "block3.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentCloud(int x, int y) {
        Segment s=new Segment(x, y, "cloud.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentG(int x, int y) {
        Segment s=new SegmentAnim(x, y, "bonus.png", new int[] {0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 2, 2, 1, 1, 1, 0, 0});
        tablicaSegmentow.add(s);
    }
    public ArrayList<Segment> pobierzPlansze() {
        return tablicaSegmentow;
    }
     public void dodajSegmentF(int x, int y) {
        Segment s=new SegmentBlockF(x, y, "fire.png", new int[] {0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3});
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentGO(int x, int y){
        Segment s=new Segment(x, y, "GO.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentE(int x,int y){
        Segment s=new SegmentE(x, y, "end.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentEmpty(int x, int y){
        Segment s=new SegmentBlock(x, y, "empty.png");
        tablicaSegmentow.add(s);
    }
}

class BudowniczyDrugi implements Budowniczy {
    public void dodajSegmentA(int x, int y) {
        Segment s=new SegmentBlock(x, y, "block1.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentB(int x, int y) {
        Segment s=new SegmentBlockV(x, y, "block2.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentC(int x, int y) {
        Segment s=new SegmentBlock(x, y, "block3.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentCloud(int x, int y) {
        Segment s=new Segment(x, y, "cloud.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentG(int x, int y) {
        Segment s=new SegmentAnim(x, y, "bonus.png", new int[] {0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 2, 2, 1, 1, 1, 0, 0});
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentF(int x, int y) {
        Segment s=new SegmentBlockF(x, y, "fire.png", new int[] {0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 2, 2, 1, 1, 1, 0, 0});
        tablicaSegmentow.add(s);
    } 
    public void dodajSegmentE(int x,int y){
        Segment s=new SegmentE(x, y, "end.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentEmpty(int x, int y){
        Segment s=new SegmentBlock(x, y, "empty.png");
        tablicaSegmentow.add(s);
    }
    public void dodajSegmentGO(int x, int y){
        Segment s=new Segment(x, y, "GO.png");
        tablicaSegmentow.add(s);
    }
    public ArrayList<Segment> pobierzPlansze() {
        return tablicaSegmentow;
    }
   
}

abstract class Mucha {

    private final double k = 0.01;
    double x, y; // pozycja muchy
    double vx, vy; // predkosc muchy

    public Mucha() {
        x = Math.random();
        y = Math.random();
        vx = k * (Math.random() - Math.random());
        vy = k * (Math.random() - Math.random());
    }

    protected abstract void setColor(Graphics g);

    protected abstract void fillOval(Graphics g, int x, int y, int width, int height);

    public final void draw(Graphics g) {
        setColor(g);
        Rectangle rc = g.getClipBounds();
        int a = (int) (x * rc.getWidth());
        int b = (int) (y * rc.getHeight());
        fillOval(g,a,b,5,5);
    }

    protected abstract void move();
}

class WektorMucha extends Mucha {

    private Random random = new Random();

    protected void move() {
        double lenght = Math.sqrt(vx*vx + vy*vy);
        double alfa = Math.acos(vx / lenght);
        double rangeMax = 2 * Math.PI;
        double rangeMin = 0;
        alfa = rangeMin + (rangeMax - rangeMin) * random.nextDouble();
        vx = lenght * Math.cos(alfa);
        vy = lenght * Math.sin(alfa);

        x += vx;
        y += vy;
		if(x<0) { x = -x; vx = -vx; }
		if(x>1) { x = 2-x;vx = -vx; }
		if(y<0) { y = -y; vy = -vy; }
		if(y>1) { y = 2-y;vy = -vy; }
    }

    @Override
    protected void setColor(Graphics g) {
        g.setColor(Color.MAGENTA);
    }

    @Override
    protected void fillOval(Graphics g, int x, int y, int width, int height) {
        g.fillOval(x,y,width,height);
    }
}

class Szablonowa7 extends JPanel implements Runnable {

    private Mucha[] ar;
    private Random random = new Random();

    public Szablonowa7() {
        this.setPreferredSize(new Dimension(640, 480));
        setLayout(new BorderLayout());
	JLabel background=new JLabel(new ImageIcon("GO.png"));
	this.add(background);
	background.setLayout(new FlowLayout());

        ar = new Mucha[300];
        for (int i = 0; i < ar.length; ++i) {
            if (random.nextBoolean()) {
                ar[i] = new OrbitujacaMucha();
            } else {
                ar[i] = new WektorMucha();
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < ar.length; ++i) {
            ar[i].draw(g);
        }
    }

    public void run() {
        while (true) {
            for (int i = 0; i < ar.length; ++i) {
                ar[i].move();
            }
            repaint();
            try {
                Thread.currentThread().sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class OrbitujacaMucha extends Mucha {

    private Random random = new Random();
    double r ,Ox, Oy, alfa;

    public OrbitujacaMucha() {
        super();
        r = random.nextDouble() / 10;
        Ox = x;
        Oy = y;
        x = x + r;
        y = y + r;
    }

    protected void move() {
        x = Ox + r * Math.sin(alfa);
        y = Oy + r * Math.cos(alfa);
        alfa = alfa + 0.1;
        if (alfa >= 2 * Math.PI)
        {
            alfa = 0;
        }
    }

    @Override
    protected void setColor(Graphics g) {
        g.setColor(Color.CYAN);
    }

    @Override
    protected void fillOval(Graphics g, int x, int y, int width, int height) {
        g.fillOval(x,y,width,height);
    }
}



public class Mario {
	public static void main(String[] args)	{
            menu();


}
        public static void menu()
        {

                JFrame f = new JFrame("Menu Mario");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setSize(300, 300);
                f.setLocationRelativeTo(null);
                f.setResizable(false);
		f.setVisible(true);


                Container c = f.getContentPane();
                f.getContentPane().setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
                JButton b1 = new JButton("Play");
                JButton b2 = new JButton("Quit");
                JButton b3 = new JButton("Scoreboard");

                Border paneEdge = BorderFactory.createMatteBorder(20,10,10,10,Color.LIGHT_GRAY);
                b1.setBorder(paneEdge);
                b2.setBorder(paneEdge);
                b3.setBorder(paneEdge);



        b1.add(new JSeparator(JSeparator.VERTICAL),BorderLayout.LINE_START);
        b2.add(new JSeparator(JSeparator.VERTICAL),BorderLayout.LINE_START);
        b3.add(new JSeparator(JSeparator.VERTICAL),BorderLayout.LINE_START);


        f.add(b1);
        f.add(b3);
        f.add(b2);



    b3.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        IPolaczenie p1 ;
        p1=Baza.getPolaczenie();
        final JOptionPane optionPane = new JOptionPane(
                p1.get(0)+"\n"+p1.get(1)+
                 "\n"+p1.get(2)+"\n"+p1.get(3)+"\n"+p1.get(4)
             );

        JDialog d = new JDialog(f, "Full ScoreBoard", true);
        d.setSize(500, 500);
        d.setLocationRelativeTo(f);
        d.setContentPane(optionPane);
        d.setVisible(true);
      }
    });


    b2.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
          System.exit(0);
      }
    });

    b1.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
           	JFrame frame = new JFrame("Mario");
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new JScrollPane(new Game("plansza01.txt")));
                //frame.getContentPane().add(new JScrollPane(new Szablonowa7()));
                frame.setBackground(null);
		frame.pack();
                frame.setResizable(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);

                frame.addWindowListener(new WindowAdapter(){
                @Override
                public void windowClosing(WindowEvent e){
                    int i=JOptionPane.showConfirmDialog(null, "Na pewno chcesz zamknac aplikacje?"+"\n"+"Tak wylaczy aplickację"
                            +"\n"+"Nie wylaczy to okno i przejdziesz do manu glownego");
                    if(i==0){
                        System.exit(0);
                    }
                    if(i==1){
                        frame.dispose();
                    }
                    if(i==2){
                        frame.dispose();
                    }
                }
            });

      }
    });


        }


   }



