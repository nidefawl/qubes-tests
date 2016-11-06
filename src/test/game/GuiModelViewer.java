package test.game;

import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.controls.*;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.models.*;
import nidefawl.qubes.models.qmodel.QModelProperties;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.render.QModelRender;

public class GuiModelViewer extends GuiWindow {
    static int nextID = 50;

    static class Setting {
        Object[] vals = new String[0];
        ComboBox box;
		 Button button1;
		 Button button2;
		 int curVal = 0;

        public Setting() {
		}
        public Setting(Gui g, String string, Object current, Object[] vals) {
            this.box = new ComboBox(g, nextID++, string);
            this.box.setValue(current);
            this.box.titleLeft=true;
            this.button1 = new Button(this.box.id, "<");
            this.button2 = new Button(this.box.id, ">");
            this.vals = vals;
        }

        void callback(int id) {
        };
    }
    static class ModelSettingControl extends Setting {

        private ModelOption option;

		/**
         * @param string
         * @param g 
         * @param string2
         */
        public ModelSettingControl(Gui g, ModelOption option) {
        	this.option = option;
    		this.curVal = option.getDefaultVal();
            this.vals = option.getOptions();
            this.box = new ComboBox(g, nextID++, option.getName());
            this.box.setValue(option.getTextVal(this.curVal));
            this.box.titleLeft=true;
            this.button1 = new Button(this.box.id, "<");
            this.button2 = new Button(this.box.id, ">");
            callback(this.curVal);
        }

		void callback(int id) {
			if (id >= 0) {
				curVal = id;
				this.box.setValue(option.getTextVal(this.curVal));
				((ModelViewer)GameBase.baseInstance).config.setOption(option.getId(), curVal);
			}
		}
    }
    static class ModelActionList extends Setting {

        private EntityModel entityModel;
		private int idx;

		/**
         * @param string
         * @param g 
         * @param string2
         */
        public ModelActionList(Gui g, EntityModel entityModel, int idx) {
        	this.entityModel = entityModel;
    		this.curVal = 0;
    		this.idx = idx;
    		List<QModelAction> actions = entityModel.getActions();
    		
    		String[] list = new String[actions.size()];
    		for (int i = 0; i < list.length; i++) {
    			list[i] = actions.get(i).name;
    		}
            this.vals = list;
            this.box = new ComboBox(g, nextID++, "Action #"+idx);
            this.box.setValue(list.length==0?"":list[this.curVal]);
            this.box.titleLeft=true;
            this.button1 = new Button(this.box.id, "<");
            this.button2 = new Button(this.box.id, ">");
            callback(this.curVal);
        }

		void callback(int id) {
			if (id >= 0) {
				curVal = id;
				List<QModelAction> list = this.entityModel.getActions();
				QModelAction act = !list.isEmpty()&&list.size()>curVal?list.get(curVal):null;
				if (act == null) {
					this.box.setValue("");
				}
				else this.box.setValue(act.name);
				
				((ModelViewer)GameBase.baseInstance).config.setAction(this.idx, act);
			}
		}
    }

    private Button            back;
    List<Setting>             list = Lists.newArrayList();
    List<Setting>             listDyn = Lists.newArrayList();
    private Setting testSetting;
	private ModelViewer viewer;
	private CheckBox checkBoxRenderMode;
	private CheckBox checkboxWireframe;
	private CheckBox checkboxNormals;
	private CheckBox checkboxBones;


    public GuiModelViewer() {
        this.viewer = (ModelViewer) GameBase.baseInstance;
    }

	public void setModel(EntityModel entityModel, final QModelRender render, final QModelProperties config) {
		config.clear();
        for (Setting s : this.list) {
        	this.remove(s.box);
        	this.remove(s.button1);
        	this.remove(s.button2);
        }
		this.list.removeAll(this.listDyn);
		this.listDyn.clear();
    	for (final ModelOption option : entityModel.getModelOptions()) {
            listDyn.add(new ModelSettingControl(this, option));
    	}
        listDyn.add(new ModelActionList(this, entityModel, 0));
        listDyn.add(new ModelActionList(this, entityModel, 1));
		this.list.addAll(this.listDyn);
        int he = layout();
	}

    @Override
    public void initGui(boolean first) {
    	setSize(340, 600);
    	setPos(20, 20);
        this.clearElements();
        this.list.clear();
        int w1 = this.width/2;
        int h = 30;
        final List<String> l = Lists.newArrayList();
        for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID; i++) {
        	l.add(EntityModel.models[i].getName());
        }
        String[] arr = l.toArray(new String[l.size()]);
        String cur = EntityModel.models[0].getName();
        list.add((this.testSetting = new Setting(this, "Select model", cur, arr) {
            void callback(int id) {
            	if (id >= 0) {
    				this.box.setValue(l.get(id));
            		this.curVal = id;
            		viewer.setModel(id);
            	}
            }
        }));
        this.checkBoxRenderMode = new CheckBox(1, "Batched GPU Skinning");
        this.checkboxWireframe = new CheckBox(1, "Show wireframe");
        this.checkboxNormals = new CheckBox(2, "Show normals");
        this.checkboxBones = new CheckBox(3, "Show bones");
        back = new Button(4, "Reload");
        int he = layout();
        this.add(back);
        this.add(checkBoxRenderMode);
        this.add(checkboxWireframe);
        this.add(checkboxNormals);
        this.add(checkboxBones);
    }

    private int layout() {
    	int w = 390;
        int w1 =w/3;
        int w2 = w1*2;
        int h = 30;

        int left = w/2-10;
        int y = 10 + 40;
        int comboH = Gui.FONT_SIZE_BUTTON+4;
        for (Setting s : list) {
            s.box.setPos(left, y);
            s.box.setSize(w2-110, comboH);
            s.box.titleWidth = left-10;
            s.button1.setPos(s.box.right()+10, y);
            s.button1.setSize(Gui.FONT_SIZE_BUTTON+2, comboH);
            s.button2.setPos(s.button1.right()+10, y);
            s.button2.setSize(Gui.FONT_SIZE_BUTTON+2, comboH);
            y += Gui.FONT_SIZE_BUTTON+2+10;
            this.add(s.box);
            this.add(s.button1);
            this.add(s.button2);
        }
        y += 20;

        int leftCB = w/3*2;
        int cbSize = Gui.FONT_SIZE_BUTTON+2;
        CheckBox[] boxes = new CheckBox[] {
        		this.checkBoxRenderMode, this.checkboxWireframe, this.checkboxNormals, this.checkboxBones
        };
        this.checkBoxRenderMode.checked = this.viewer.renderBatchedMode;
        for (int i = 0; i < boxes.length; i++) {
        	CheckBox box = boxes[i];
        	box.setPos(leftCB, y);
        	box.setSize(cbSize, cbSize);
        	box.titleWidth = leftCB-10;
            y += cbSize+10;
        }
        y += 25;
        back.setPos(30, y);
        back.setSize(w-40, h);
        y += h;
        y+=20;
    	setSize(w+20, y);
        return y;
	}

	public void render(float fTime, double mX, double mY) {
		this.checkboxNormals.draw = !this.viewer.renderBatchedMode;
		this.checkboxWireframe.draw = !this.viewer.renderBatchedMode;
        super.renderButtons(fTime, mX, mY);

    }
    @Override
    public String getTitle() {
    	return "model preview";
    }

    public boolean onGuiClicked(AbstractUI element) {
    	if (element instanceof CheckBox) {

    		((CheckBox)element).checked = !((CheckBox)element).checked;
    	}
    	if (element == this.checkboxNormals) {
    		this.viewer.showNormals = this.checkboxNormals.checked;
    		return true;
    	}
    	if (element == this.checkboxBones) {
    		this.viewer.showBones = this.checkboxBones.checked;
    		return true;
    	}
    	if (element == this.checkboxWireframe) {
    		this.viewer.showWireframe = this.checkboxWireframe.checked;
    		return true;
    	}
    	if (element == this.checkBoxRenderMode) {
    		this.viewer.renderBatchedMode = this.checkBoxRenderMode.checked;
    		return true;
    	}
        for (int i = 0; i < this.list.size(); i++) {
            final Setting s = this.list.get(i);
            if (s.button1 == element) {
            	s.curVal--;
            	if (s.curVal < 0) {
            		s.curVal = s.vals.length-1;
            	}
        		s.callback(s.curVal);
        		return true;
            }
            if (s.button2 == element) {
            	s.curVal++;
            	if (s.curVal >= s.vals.length) {
            		s.curVal = 0;
            	}
        		s.callback(s.curVal);
        		return true;
            }
            if (s.box == element) {
                if (s.box.onClick(this)) {
                    setPopup(new ComboBox.ComboBoxList(new ComboBox.CallBack() {
                        @Override
                        public void call(ComboBoxList c, int id) {

                            GuiModelViewer.this.setPopup(null);
                            if (id < 0 || id >= s.vals.length)
                                return;
                            s.box.setValue(s.vals[id]);
                            s.callback(id);
                        }
                    }, this, s.box, s.vals));
                }
            }
        }
        if (element == back) {
        	EntityModelManager.getInstance().reload();
        }
        return true;
    }


}
