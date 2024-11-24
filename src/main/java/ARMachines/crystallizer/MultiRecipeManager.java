package ARMachines.crystallizer;

import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.multiblockCore.MultiblockRecipeManager;
import ARLib.utils.MachineRecipe;
import ARLib.utils.recipePart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public    class MultiRecipeManager<T extends EntityMultiblockMaster> {

    public List<MultiblockRecipeManager<T>> recipeManagers;

    public MultiRecipeManager(List<MultiblockRecipeManager<T>> recipeManagers) {
        this.recipeManagers = recipeManagers;
    }

    public List<recipePart> getReservedInputs() {
        List<recipePart> reservedInputs = new ArrayList<>();
        for (MultiblockRecipeManager<T> i : recipeManagers) {
            if (i.currentRecipe != null) {
                reservedInputs.addAll(i.currentRecipe.inputs);
            }
        }
        return reservedInputs;
    }

    public List<recipePart> getReservedOutputs() {
        List<recipePart> reservedOutputs = new ArrayList<>();
        for (MultiblockRecipeManager<T> i : recipeManagers) {
            if (i.currentRecipe != null) {
                reservedOutputs.addAll(i.currentRecipe.outputs);
            }
        }
        return reservedOutputs;
    }

    public List<recipePart> addedIdsAndNumsIgnoreP(List<recipePart> list){
        Map <String, Integer> ids_nums = new HashMap<>();
        for (recipePart i : list){
            if(ids_nums.containsKey(i.id)){
                ids_nums.put(i.id, ids_nums.get(i.id)+i.num);
            }
            else{
                ids_nums.put(i.id, i.num);
            }
        }
        List<recipePart> list2 = new ArrayList<>();
        for (String i : ids_nums.keySet()){
            int num = ids_nums.get(i);
            recipePart newPart = new recipePart(i,num,0);
            list2.add(newPart);
        }
        return list2;
    }

    public void scanFornewRecipe(MultiblockRecipeManager<T> m) {
        List<recipePart> reservedOutputs = getReservedOutputs();
        List<recipePart> reservedInputs = getReservedInputs();

        for (int i = 0; i < m.recipes.size(); i++) {
            MachineRecipe r = m.recipes.get(i);
            List<recipePart> totalRequiredInputs = new ArrayList<>();
            totalRequiredInputs.addAll(reservedInputs);
            List<recipePart> totalRequiredOutputs = new ArrayList<>(reservedOutputs);
            totalRequiredInputs.addAll(r.inputs);
            totalRequiredOutputs.addAll(r.outputs);
            totalRequiredOutputs = addedIdsAndNumsIgnoreP(totalRequiredInputs);
            totalRequiredInputs = addedIdsAndNumsIgnoreP(totalRequiredInputs);


            if (m.master.hasinputs(totalRequiredInputs) && m.master.canFitOutputs(totalRequiredOutputs)) {
                m.currentRecipe = r.copy();
                m.currentRecipe.compute_actual_output_nums();
                //System.out.println("set recipe");
                break;
            }
        }

    }

    public List<Boolean> update() {
        List<Boolean> rets = new ArrayList<>();

        for (MultiblockRecipeManager<T> i : recipeManagers) {
            if (i.currentRecipe == null) {
                scanFornewRecipe(i);
                rets.add(false);
            } else {
                List<recipePart> reservedOutputs = getReservedOutputs();
                List<recipePart> reservedInputs = getReservedInputs();

                if (i.master.hasinputs(reservedInputs) && i.master.canFitOutputs(reservedOutputs)) {
                    if (i.master.getTotalEnergyStored() >= i.currentRecipe.energyPerTick) {
                        ++i.progress;
                        //System.out.println(i.progress);
                        i.master.consumeEnergy(i.currentRecipe.energyPerTick);
                        if (i.progress == i.currentRecipe.ticksRequired) {
                            i.master.consumeInput(i.currentRecipe.inputs, false);
                            i.master.produceOutput(i.currentRecipe.outputs);
                            i.reset();
                        }
                        rets.add(true);
                    }else {
                        rets.add(false);
                    }
                } else {
                    i.reset();
                    rets.add(false);
                }
            }
        }
        return rets;
    }
}
