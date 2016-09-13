function App() {
    this.parseUrl = 'api/parse/';
    this.loadUrl = 'api/load/';
    this.saveUrl = 'api/save/';
};

App.prototype.init = function() {
    var self = this;

    var sessionId = location.hash.substring(1);
    if (sessionId) {
        self.load(sessionId);
    }

    $('#parse').click(function() {
        self.parseExpression(self.parseUrl);
    });

    $('#save').click(function() {
        self.parseExpression(self.saveUrl, function(id){
            window.location = '/#' + id;
        });
    });

    var reqTimer;
    $('#src').on('keyup', function() {
        if (reqTimer) {
            clearTimeout(reqTimer);
        }

        reqTimer = setTimeout(function(){
            self.parseExpression(self.parseUrl);
        }, 1000);
    });

    self.parserEditor = self.initEditor("grammar");
    self.lexerEditor = self.initEditor("lexer");
};

App.prototype.loadRules = function(rules, rule) {
    var startRuleSel = $('#startRule');
    startRuleSel.empty();
    for(var o in rules) {
        startRuleSel.append('<option>' + rules[o] + '</option>');
    }

    startRuleSel.val(rule);
};

App.prototype.showGrammarErrors = function(errors) {
    var errorNotifications = errors.map(function(e) {
    return {
        column: e.col,
        row: e.line - 1,
        type: e.errType,
        text: e.message
    }});

    this.parserEditor.getSession().setAnnotations(errorNotifications);
};

App.prototype.showParseMessages = function(messages) {
    if (messages.length == 0) {
        $('#parseMessages').hide();
    }
    else
    {
        var msg = messages.map(function(m) { return "line " + m.line + ", col: " + m.col + " : " + m.message; }).join("<br>");
        $('#parseMessages').text(msg).show();
    }
}

App.prototype.parseExpression = function(url, callback) {
    var self = this;
    var grammar = self.parserEditor.getValue();
    var lexer = self.lexerEditor.getValue();
    var src = $('#src').val();
    var startRuleSel = $('#startRule');
    $.post(url, { grammar: grammar, lexer: lexer, src: src, rule: startRuleSel.val() }, function(data){
        if (data.tree && data.parsedGrammar.rules) {
            self.loadRules(data.parsedGrammar.rules, data.rule);
            self.draw(self.getTreeModel(data.tree));
            $('#grammarError').hide();
            self.showGrammarErrors(data.parsedGrammar.warnings);
            self.showParseMessages(data.messages);
        } else {
            $('#grammarError').show();
            self.showGrammarErrors(data.errors);
            self.draw(null);
        }

        if (callback && data.id) {
            callback(data.id);
        }

    }).fail(function(err){
        $('#grammarError').show();
    });
}

App.prototype.load = function(id) {
    var self = this;
    $.get(self.loadUrl + id, {}, function(res){
        self.parserEditor.setValue(res.grammar, -1);
        self.lexerEditor.setValue(res.lexer, -1);
        $('#src').val(res.src);
//        self.loadRules(res.rules.split(','), res.rule);
    });
};

App.prototype.draw = function(tree) {
    $('#tree').jstree();
    $('#tree').jstree(true).settings.core.data = tree;
    $('#tree').jstree(true).refresh();
};

App.prototype.initEditor = function(elId) {
    var editor = ace.edit(elId);
    editor.setTheme("ace/theme/chrome");
    editor.getSession().setMode("ace/mode/antlr4");

    return editor;
};

App.prototype.getTreeModel = function(tree) {
    var children = [];
    for(var c in tree.children) {
        children.push(this.getTreeModel(tree.children[c]));
    }

    var node = { text: tree.rule + ': ' + tree.text, children: children, state: { opened: true }, icon: false };
    if (tree.hasError) {
        node.icon = "glyphicon glyphicon-remove red";
    }

    return node;
};


$(function(){
    var app = new App();
    app.init();
})