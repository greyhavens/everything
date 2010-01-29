//
// $Id$

package com.threerings.everything.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;

import com.threerings.everything.client.EditorService;
import com.threerings.everything.client.GameCodes;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.Created;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.GameLogic;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link EditorService}.
 */
public class EditorServlet extends EveryServiceServlet
    implements EditorService
{
    // from interface EditorService
    public List<Category> loadCategories (int parentId) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        // load up the categories in question and resolve their names
        List<Category> cats = sort(Lists.newArrayList(_thingRepo.loadCategories(parentId)));
        return _playerLogic.resolveNames(cats, editor.getName());
    }

    // from interface EditorService
    public List<Category> loadMySeries () throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        return _playerLogic.resolveNames(
            Lists.newArrayList(_thingRepo.loadCategoriesBy(editor.userId)));
    }

    // from interface EditorService
    public int createCategory (Category category) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        category.state = Category.State.IN_DEVELOPMENT;
        category.creator = editor.getName();
        category.categoryId = _thingRepo.createCategory(category);
        _adminLogic.noteAction(editor, "created", category);
        return category.categoryId;
    }

    // from interface EditorService
    public void updateCategory (Category category) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Category ocategory = _thingRepo.loadCategory(category.categoryId);
        PlayerRecord editor = checkEditor(ocategory);
        boolean activeChanged = (ocategory.isActive() != category.isActive());

        // only editors can activate and deactivate categories
        if (activeChanged) {
            requireAdmin();
        }

        // if we're activating...
        if (!ocategory.isActive() && category.isActive()) {
            // possibly announce this new series
            if (ocategory.paid == 0) {
                Category[] cats = _gameLogic.resolveCategories(category.categoryId);
                _playerRepo.recordFeedItem(category.creator.userId, FeedItem.Type.NEW_SERIES, 0,
                                           Category.getHierarchy(cats));
            }
            // and check to see if the creator has been paid for all of its things
            int things = _thingRepo.getThingCount(ocategory.categoryId);
            if (ocategory.paid < things) {
                int payout = (things - ocategory.paid) * GameCodes.COINS_PER_CREATED_CARD;
                _playerRepo.grantCoins(category.getCreatorId(), payout);
                log.info("Paid " + category.creator + " " + payout + " coins for '" +
                         category.name + "' series.");
                ocategory.paid = things; // update the paid things count
            }
        }

        // fill in user updatable values: name, state and parentId
        ocategory.name = category.name;
        ocategory.state = category.state;
        ocategory.parentId = category.parentId;

        // actually update the category
        _thingRepo.updateCategory(ocategory);

        String action;
        if (activeChanged) {
            action = (category.isActive()) ? "activated" : "deactivated";
            // if a category changed active state, queue a reload of the thing index
            log.info("Category active status changed. TODO: reload thing index.");
        } else {
            action = "updated";
        }

        // note that this action was taken
        _adminLogic.noteAction(editor, action, category);
    }

    // from interface EditorService
    public void deleteCategory (int categoryId) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Category category = _thingRepo.loadCategory(categoryId);
        PlayerRecord editor = checkEditor(category);

        // make sure the category can be deleted
        if (_thingRepo.loadCategories(categoryId).iterator().hasNext()) {
            throw new ServiceException(EditorService.E_CAT_HAS_SUBCATS);
        }
        if (_thingRepo.getThingCount(categoryId) > 0) {
            throw new ServiceException(EditorService.E_CAT_HAS_THINGS);
        }

        // delete the category
        _thingRepo.deleteCategory(category);

        // note that this action was taken
        _adminLogic.noteAction(editor, "deleted", category);
    }

    // from interface EditorService
    public CategoryComment postComment (int categoryId, String message) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        Category category = _thingRepo.loadCategory(categoryId);
        if (category == null) {
            log.warning("Requested to comment on non-existent series", "who", editor.who(),
                        "catId", categoryId);
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }

        // record this comment to the thing repository
        CategoryComment comment = _thingRepo.recordComment(categoryId, editor.userId, message);
        comment.commentor = editor.getName();
        return comment;
    }

    // from interface EditorService
    public SeriesResult loadSeries (int categoryId) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        SeriesResult result = new SeriesResult();
        result.categories = _gameLogic.resolveCategories(categoryId);
        Category series = result.categories[result.categories.length-1];
        series.creator = _playerRepo.loadPlayerName(series.creator.userId);
        result.comments = Lists.newArrayList(_thingRepo.loadComments(categoryId));
        _playerLogic.resolveNames(result.comments, series.creator, editor.getName());
        result.things = sort(Lists.newArrayList(_thingRepo.loadThings(categoryId)));
        return result;
    }

    // from interface EditorService
    public int createThing (Thing thing) throws ServiceException
    {
        PlayerRecord editor = requireEditor();

        // make sure the series exists, they own it and that it's not active (these should be
        // checked on the client as well)
        Category series = _thingRepo.loadCategory(thing.categoryId);
        if (series == null) {
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }
        if (series.isActive() || (series.getCreatorId() != editor.userId && !getUser().isAdmin())) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }

        // do the deed
        thing.creator = editor.getName();
        thing.thingId = _thingRepo.createThing(thing);
        _adminLogic.noteAction(editor, "created", thing);
        return thing.thingId;
    }

    // from interface EditorService
    public void updateThing (Thing thing) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Thing othing = _thingRepo.loadThing(thing.thingId);
        PlayerRecord editor = checkEditor(othing);

        // TODO: if this thing is moving categories, validate that

        // actually update the thing
        thing.creator = othing.creator;
        _thingRepo.updateThing(thing);

        // note that this action was taken
        _adminLogic.noteAction(editor, "updated", thing);
    }

    // from interface EditorService
    public void deleteThing (int thingId) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Thing thing = _thingRepo.loadThing(thingId);
        PlayerRecord editor = checkEditor(thing);

        // TODO: check whether cards are using this thing

        // delete the thing
        _thingRepo.deleteThing(thing);

        // note that this action was taken
        _adminLogic.noteAction(editor, "deleted", thing);
    }

    // from interface EditorService
    public String slurpImage (String imgurl) throws ServiceException
    {
        requireEditor();
        try {
            return _mediaLogic.processImage(new URL(imgurl));
        } catch (MalformedURLException mue) {
            throw new ServiceException(E_INVALID_URL);
        }
    }

    protected PlayerRecord checkEditor (Created created)
        throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        if (created == null) {
            log.warning("Requested to edit non-existent object", "who", editor.who());
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }
        OOOUser user = getUser();
        if (!user.isAdmin() && created.getCreatorId() != user.userId) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }
        return editor;
    }

    protected static <T extends Comparable<T>> List<T> sort (List<T> list) {
        Collections.sort(list);
        return list;
    }

    @Inject protected AdminLogic _adminLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected MediaLogic _mediaLogic;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected ThingRepository _thingRepo;
}
