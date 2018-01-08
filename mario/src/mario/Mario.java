package mario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

//********************* element planszy
//klasa bazowa dla segmentow
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
}
//************************* postac gracza
class Sprite {
	private static final Image img = new ImageIcon("Mario.png").getImage();
		
	private int[] anim = {0, 1, 2, 1};
	private int frame = 2;		// klatka animacji
	private boolean mirror = false; // postac patrzy w lewo/ prawo
	private int moving = 0;		// ruch w poziomie
	private int jumping = 0; 	// ruch w pionie
	private final ArrayList<Segment> plansza;

	private int x=150, y=100; 	// pozycja na ekranie
	private final int W=16, H=27;// wysokosc i szerokosc sprite'a
	
	public Sprite(ArrayList<Segment> pl) { plansza=pl; }

	public int getX() { return x; }
	public int getY() { return y; }
	public int getBottomY() { return y+H; }

	public void jump() {		// poruszanie postacia
		if(jumping == 0) jumping = 10;
	}
	public boolean isJumping() { return jumping>0; }
	public boolean jumpingDown() { return jumping<0; }
	public void stopJump() { jumping=0; }
	public void stopMove() { moving=0; }

	public void left() {
		moving = -3;
		mirror = false;
	}
	public void right() {
		moving = 3;
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
		setPreferredSize(new Dimension(2000, 2000));
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
    public void dodajSegmentG(int x, int y) {
        Segment s=new SegmentAnim(x, y, "bonus.png", new int[] {0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 2, 2, 1, 1, 1, 0, 0});
        tablicaSegmentow.add(s);
    }
    public ArrayList<Segment> pobierzPlansze() {
        return tablicaSegmentow;
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
    public void dodajSegmentG(int x, int y) {
        Segment s=new SegmentAnim(x, y, "bonus.png", new int[] {0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 2, 2, 1, 1, 1, 0, 0});
        tablicaSegmentow.add(s);
    }
    public ArrayList<Segment> pobierzPlansze() {
        return tablicaSegmentow;
    }
}

public class Mario {
	public static void main(String[] args)	{
		JFrame frame = new JFrame("Mario");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new JScrollPane(new Game("plansza01.txt")));
		frame.pack();
		frame.setVisible(true);
	}
}

